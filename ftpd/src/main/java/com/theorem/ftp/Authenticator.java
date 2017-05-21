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

// Interface for running external authentication process.
// The external application need to use this as the entry method for
// the ftp daemon.
// The class "auth" with the method "authorize" must exist for this
// to be used.  The authorize method returns true if the authentication
// worked, false if it failed.

package com.theorem.ftp;

public interface Authenticator {
    
    /**
     * Authenticate somehow.
     *
     * @param name      Entity name.
     * @param password  Entity password.
     * @param configDir Configuration directory for the FTP server.
     * @param log       Log object.  Usage: log.logMsg(String msg).
     *                  The server prepends the thread name to the message to track sessions.
     *                  You can do this by <kbd>log.logMsg(Thread.currentThread().getName() + msg);</kbd>
     * @return true if the entity exists and uses the given password, false otherwise.
     */
    boolean authenticate(String name, String password, String configDir, LogMsg log);
    
}
