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
* Correct path separators between the Windows & Unix world.
*/
public class CorrectPath
{
	public char sep;
	public String newPath(String path)
	{
		sep = System.getProperty("file.separator").charAt(0);
		path = path.replace('\\', sep);
		path = path.replace('/', sep);
		return path;
	}

	public String fixPath(String path)
	{
		return newPath(path);
	}
}

