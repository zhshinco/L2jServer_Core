/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.serverpackets;

/**
 * @author Dezmond_snz
 * Format: cdddsdd
 */
public class ConfirmDlg extends ServerBasePacket
{
	private static final String _S__ED_CONFIRMDLG = "[S] ed ConfirmDlg";
	private int _requestId;
	private String _Name;

	public ConfirmDlg(int requestId, String requestorName)
	{
		_requestId = requestId;
		_Name = requestorName;
	}

	final void runImpl()
	{
		// no long-running tasks
	}
	
	final void writeImpl()
	{
		writeC(0xed);
		writeD(_requestId);
		writeD(0x02); // ??
		writeD(0x00); // ??
		writeS(_Name);
		writeD(0x01); // ??
		writeD(0x00); // ??
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	public String getType()
	{
		return _S__ED_CONFIRMDLG;
	}
}
