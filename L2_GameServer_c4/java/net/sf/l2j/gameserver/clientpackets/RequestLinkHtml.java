/**
 * 
 */
package net.sf.l2j.gameserver.clientpackets;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.ClientThread;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;

/**
 * @author zabbix
 * Lets drink to code!
 */
public class RequestLinkHtml extends ClientBasePacket
{
	private static Logger _log = Logger.getLogger(RequestLinkHtml.class.getName());
	private static final String REQUESTLINKHTML__C__20 = "[C] 20 RequestLinkHtml";
	private String _link;

	public RequestLinkHtml(ByteBuffer buf, ClientThread client)
	{
		super(buf,client);
	}
	
	public void runImpl()
	{
		L2PcInstance actor = getClient().getActiveChar();
		if(actor == null)
			return;
		
		_link = readS();
		
		if(_link.contains("..") || !_link.contains(".htm"))
		{
			_log.warning("[RequestLinkHtml] hack? link contains prohibited characters: '"+_link+"', skipped");
			return;
		}
		
		NpcHtmlMessage msg = new NpcHtmlMessage(0);
		msg.setFile(_link);
		
		sendPacket(msg);
	}
	
	public String getType()
	{
		return REQUESTLINKHTML__C__20;
	}
}
