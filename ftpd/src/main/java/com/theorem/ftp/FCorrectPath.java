/*
* FTP Server Daemon
* Copyright (C) 2000 Michael Lecuyer. All Rights reserved.
* 
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
* 
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
* 
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*
* Or see [http://www.gnu.org/copyleft/lesser.html].
*/

package com.theorem.ftp;

/**
* Takes the corrected path (correct for the OS) and makes sure there
*aren't extra separator chars, and the path doesn't end in the sep char.
*/
public class FCorrectPath extends CorrectPath
{
	/**
	* Fix a path to be OS specific (Win/Unix).
	*
	* @param path path in either Unix or Windows form.
	* @return path corrected for the current running platform.
	*/
	public String fixit(String path)
	{
		path = super.newPath(path);

		int plen = path.length();
		char lastc;

		StringBuffer sbpath = new StringBuffer(plen);
		sbpath.append((lastc = path.charAt(0)));

		for (int i = 1; i < plen; i++)
		{
			char cat = path.charAt(i);
			if (cat == super.sep)
				if (lastc == super.sep)
				{
					lastc = cat;
					continue;
				}
			lastc = cat;
			sbpath.append(cat);
		}

		if (sbpath.charAt(sbpath.length()-1) == sep)		// remove trailing /
			sbpath.setLength(sbpath.length()-1);

		return sbpath.toString();
	 }
}
