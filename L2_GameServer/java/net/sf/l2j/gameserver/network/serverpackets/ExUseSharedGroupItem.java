/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.network.serverpackets;

/**
 * Format: ch dddd
 * @author KenM
 */
public class ExUseSharedGroupItem extends L2GameServerPacket
{
	private static final String _S__FE_49_EXUSESHAREDGROUPITEM = "[S] FE:4a ExUseSharedGroupItem";
	private int _unk1, _unk2, _unk3, _unk4;

	public ExUseSharedGroupItem(int unk1, int unk2, int unk3, int unk4)
	{
		_unk1 = unk1;
		_unk2 = unk2;
		_unk3 = unk3;
		_unk4 = unk4;
	}

	/**
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x4a);

		writeD(_unk1);
		writeD(_unk2);
		writeD(_unk3);
		writeD(_unk4);
	}

	/**
	 * @see net.sf.l2j.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_49_EXUSESHAREDGROUPITEM;
	}

}
