/**
 * 
 */
package net.sf.l2j.gameserver.taskmanager;

import static net.sf.l2j.gameserver.taskmanager.TaskTypes.TYPE_FIXED_SHEDULED;
import static net.sf.l2j.gameserver.taskmanager.TaskTypes.TYPE_GLOBAL_TASK;
import static net.sf.l2j.gameserver.taskmanager.TaskTypes.TYPE_NONE;
import static net.sf.l2j.gameserver.taskmanager.TaskTypes.TYPE_SHEDULED;
import static net.sf.l2j.gameserver.taskmanager.TaskTypes.TYPE_SPECIAL;
import static net.sf.l2j.gameserver.taskmanager.TaskTypes.TYPE_STARTUP;
import static net.sf.l2j.gameserver.taskmanager.TaskTypes.TYPE_TIME;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.taskmanager.tasks.TaskCleanUp;
import net.sf.l2j.gameserver.taskmanager.tasks.TaskJython;
import net.sf.l2j.gameserver.taskmanager.tasks.TaskOlympiadSave;
import net.sf.l2j.gameserver.taskmanager.tasks.TaskRecom;
import net.sf.l2j.gameserver.taskmanager.tasks.TaskRestart;
import net.sf.l2j.gameserver.taskmanager.tasks.TaskSevenSignsUpdate;
import net.sf.l2j.gameserver.taskmanager.tasks.TaskShutdown;

/**
 * @author Layane
 *
 */
public final class TaskManager
{
    protected static final Logger _log = Logger.getLogger(TaskManager.class.getName());
    private static TaskManager _instance;

    protected static final String[] SQL_STATEMENTS = {
                                                      "SELECT id,task,type,last_activation,param1,param2,param3 FROM global_tasks",
                                                      "UPDATE global_tasks SET last_activation=? WHERE id=?",
                                                      "SELECT id FROM global_tasks WHERE task=?",
                                                      "INSERT INTO global_tasks (task,type,last_activation,param1,param2,param3) VALUES(?,?,?,?,?,?)"};

    private final FastMap<Integer, Task> _tasks = new FastMap<Integer, Task>();
    protected final FastList<ExecutedTask> _currentTasks = new FastList<ExecutedTask>();

    public class ExecutedTask implements Runnable
    {
        int _id;
        long _lastActivation;
        Task _task;
        TaskTypes _type;
        String[] _params;
        ScheduledFuture _scheduled;

        public ExecutedTask(Task task, TaskTypes type, ResultSet rset) throws SQLException
        {
            _task = task;
            _type = type;
            _id = rset.getInt("id");
            _lastActivation = rset.getLong("last_activation");
            _params = new String[] {rset.getString("param1"), rset.getString("param2"),
                                    rset.getString("param3")};
        }

        public void run()
        {
            _task.onTimeElapsed(this);

            _lastActivation = System.currentTimeMillis();

            java.sql.Connection con = null;

            try
            {
                con = L2DatabaseFactory.getInstance().getConnection();
                PreparedStatement statement = con.prepareStatement(SQL_STATEMENTS[1]);
                statement.setLong(1, _lastActivation);
                statement.setInt(2, _id);
                statement.executeUpdate();
                statement.close();
            }
            catch (SQLException e)
            {
                _log.warning("cannot updated the Global Task " + _id + ": " + e.getMessage());
            }
            finally
            {
                try
                {
                    con.close();
                }
                catch (Exception e)
                {
                }
            }

            if (_type == TYPE_SHEDULED || _type == TYPE_TIME)
            {
                stopTask();
            }
        }

        public boolean equals(Object object)
        {
            return _id == ((ExecutedTask) object)._id;
        }

        public Task getTask()
        {
            return _task;
        }

        public TaskTypes getType()
        {
            return _type;
        }

        public int getId()
        {
            return _id;
        }

        public String[] getParams()
        {
            return _params;
        }

        public long getLastActivation()
        {
            return _lastActivation;
        }

        public void stopTask()
        {
            _task.onDestroy();

            if (_scheduled != null) _scheduled.cancel(true);

            _currentTasks.remove(this);
        }

    }

    public static TaskManager getInstance()
    {
        if (_instance == null)
        {
            _instance = new TaskManager();
        }
        return _instance;
    }

    public TaskManager()
    {
        initializate();
        startAllTasks();
    }

    private void initializate()
    {
        registerTask(new TaskCleanUp());
        registerTask(new TaskJython());
        registerTask(new TaskOlympiadSave());
        registerTask(new TaskRecom());
        registerTask(new TaskRestart());
        registerTask(new TaskSevenSignsUpdate());
        registerTask(new TaskShutdown());
    }

    public void registerTask(Task task)
    {
        int key = task.getName().hashCode();
        if (!_tasks.containsKey(key))
        {
            _tasks.put(key, task);
            task.initializate();
        }
    }

    private void startAllTasks()
    {
        java.sql.Connection con = null;
        try
        {
            try
            {
                con = L2DatabaseFactory.getInstance().getConnection();
                PreparedStatement statement = con.prepareStatement(SQL_STATEMENTS[0]);
                ResultSet rset = statement.executeQuery();

                while (rset.next())
                {
                    Task task = _tasks.get(rset.getString("task").trim().toLowerCase().hashCode());

                    if (task == null) continue;

                    TaskTypes type = TaskTypes.valueOf(rset.getString("type"));

                    if (type != TYPE_NONE)
                    {
                        ExecutedTask current = new ExecutedTask(task, type, rset);
                        if (launchTask(current)) _currentTasks.add(current);
                    }

                }

                rset.close();
                rset.close();

            }
            catch (Exception e)
            {
                _log.severe("error while loading Global Task table " + e);
                e.printStackTrace();
            }

        }
        finally
        {
            try
            {
                con.close();
            }
            catch (Exception e)
            {
            }
        }
    }

    private boolean launchTask(ExecutedTask task)
    {
        final ThreadPoolManager scheduler = ThreadPoolManager.getInstance();
        final TaskTypes type = task.getType();

        if (type == TYPE_STARTUP)
        {
            task.run();
            return false;
        }
        else if (type == TYPE_SHEDULED)
        {
            long delay = Long.valueOf(task.getParams()[0]);
            task._scheduled = scheduler.scheduleGeneral(task, delay);
            return true;
        }
        else if (type == TYPE_FIXED_SHEDULED)
        {
            long delay = Long.valueOf(task.getParams()[0]);
            long interval = Long.valueOf(task.getParams()[1]);

            task._scheduled = scheduler.scheduleGeneralAtFixedRate(task, delay, interval);
            return true;
        }
        else if (type == TYPE_TIME)
        {
            try
            {
                Date desired = DateFormat.getInstance().parse(task.getParams()[0]);
                long diff = desired.getTime() - System.currentTimeMillis();
                if (diff >= 0)
                {
                    task._scheduled = scheduler.scheduleGeneral(task, diff);
                    return true;
                }
                _log.info("Task " + task.getId() + " is obsoleted.");
            }
            catch (Exception e)
            {
            }
        }
        else if (type == TYPE_SPECIAL)
        {
            ScheduledFuture result = task.getTask().launchSpecial(task);
            if (result != null)
            {
                task._scheduled = result;
                return true;
            }
        }
        else if (type == TYPE_GLOBAL_TASK)
        {
            long interval = Long.valueOf(task.getParams()[0]) * 86400000L;
            String[] hour = task.getParams()[1].split(":");

            if (hour.length != 3)
            {
                _log.warning("Task " + task.getId() + " has incorrect parameters");
                return false;
            }

            Calendar check = Calendar.getInstance();
            check.setTimeInMillis(task.getLastActivation() + interval);

            Calendar min = Calendar.getInstance();
            try
            {
                min.set(Calendar.HOUR_OF_DAY, Integer.valueOf(hour[0]));
                min.set(Calendar.MINUTE, Integer.valueOf(hour[1]));
                min.set(Calendar.SECOND, Integer.valueOf(hour[2]));
            }
            catch (Exception e)
            {
                _log.warning("Bad parameter on task " + task.getId() + ": " + e.getMessage());
                return false;
            }

            long delay = min.getTimeInMillis() - System.currentTimeMillis();

            if (check.after(min) || delay < 0)
            {
                delay += interval;
            }

            task._scheduled = scheduler.scheduleGeneralAtFixedRate(task, delay, interval);

            return true;
        }

        return false;
    }

    public static boolean addUniqueTask(String task, TaskTypes type, String param1, String param2,
                                        String param3)
    {
        return addUniqueTask(task, type, param1, param2, param3, 0);
    }

    public static boolean addUniqueTask(String task, TaskTypes type, String param1, String param2,
                                        String param3, long lastActivation)
    {
        java.sql.Connection con = null;

        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement(SQL_STATEMENTS[2]);
            statement.setString(1, task);
            ResultSet rset = statement.executeQuery();

            if (!rset.next())
            {
                statement = con.prepareStatement(SQL_STATEMENTS[3]);
                statement.setString(1, task);
                statement.setString(2, type.toString());
                statement.setLong(3, lastActivation);
                statement.setString(4, param1);
                statement.setString(5, param2);
                statement.setString(6, param3);
                statement.execute();
            }

            rset.close();
            statement.close();

            return true;
        }
        catch (SQLException e)
        {
            _log.warning("cannot add the unique task: " + e.getMessage());
        }
        finally
        {
            try
            {
                con.close();
            }
            catch (Exception e)
            {
            }
        }

        return false;
    }

    public static boolean addTask(String task, TaskTypes type, String param1, String param2,
                                  String param3)
    {
        return addTask(task, type, param1, param2, param3, 0);
    }

    public static boolean addTask(String task, TaskTypes type, String param1, String param2,
                                  String param3, long lastActivation)
    {
        java.sql.Connection con = null;

        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement(SQL_STATEMENTS[3]);
            statement.setString(1, task);
            statement.setString(2, type.toString());
            statement.setLong(3, lastActivation);
            statement.setString(4, param1);
            statement.setString(5, param2);
            statement.setString(6, param3);
            statement.execute();

            statement.close();
            return true;
        }
        catch (SQLException e)
        {
            _log.warning("cannot add the task:  " + e.getMessage());
        }
        finally
        {
            try
            {
                con.close();
            }
            catch (Exception e)
            {
            }
        }

        return false;
    }

}
