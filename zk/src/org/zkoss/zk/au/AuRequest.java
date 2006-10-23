/* AuRequest.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Tue May 31 11:31:13     2005, Created by tomyeh@potix.com
}}IS_NOTE

Copyright (C) 2005 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package org.zkoss.zk.au;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.ComponentNotFoundException;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.PageCtrl;
import org.zkoss.zk.ui.sys.ComponentsCtrl;
import org.zkoss.zk.au.impl.*;

/**
 * A request sent from the client to {@link org.zkoss.zk.ui.sys.UiEngine}.
 *
 * @author <a href="mailto:tomyeh@potix.com">tomyeh@potix.com</a>
 */
public class AuRequest {
	private final Desktop _desktop;
	private Page _page;
	private Component _comp;
	/** Component's UUID. Used only if _comp is not specified directly. */
	private final String _uuid;
	private final Command _cmd;
	private final String[] _data;

	//-- static --//
	private static final Map _cmds = new HashMap();

	/** Returns whether the specified request name is valid.
	 * A request is a  event that are sent from the browser.
	 *
	 * <p>An request name is the ID of a command
	 * ({@link Command#getId}) which starts with "on".
	 */
	public static final boolean hasRequest(String cmdnm) {
		return _cmds.containsKey(cmdnm);
	}
	/** Returns the command of the specified name.
	 * @exception CommandNotFoundException if the command is not found
	 */
	public static final Command getCommand(String name) {
		final Command cmd = (Command)_cmds.get(name);
		if (cmd == null)
			throw new CommandNotFoundException("Unknown command: "+name);
		return cmd;
	}
	/** Adds a new command. Called only by Command's contructor. */
	/*package*/ static final void addCommand(Command cmd) {
		if (_cmds.put(cmd.getId(), cmd) != null)
			throw new InternalError("Replicated command: "+cmd);
	}

	/** Constructor for a request sent from a component.
	 * Since we cannot invoke {@link Desktop#getComponentByUuid} without
	 * activating an execution, we have to use this method.
	 *
	 * @param desktop the desktop containing the component; never null.
	 * @param uuid the component ID (never null)
	 * @param cmd the command; never null.
	 * @param data the data; might be null.
	 */
	public AuRequest(Desktop desktop, String uuid, Command cmd, String[] data) {
		if (desktop == null || uuid == null || cmd == null)
			throw new IllegalArgumentException("null");

		_desktop = desktop;
		_uuid = uuid;
		_cmd = cmd;
		_data = data;
	}
	/** Constructor for a general request sent from client.
	 * This is usully used to ask server to log or report status.
	 *
	 * @param cmd the command; never null.
	 * @param data the data; might be null.
	 */
	public AuRequest(Desktop desktop, Command cmd, String[] data) {
		if (desktop == null || cmd == null)
			throw new IllegalArgumentException("null");
		_desktop = desktop;
		_uuid = null;
		_cmd = cmd;
		_data = data;
	}

	/** Returns the desktop; never null. */
	public final Desktop getDesktop() {
		return _desktop;
	}
	/** Returns the page that this request is applied for, or null
	 * if this reqeuest is a general request -- regardless any page or
	 * component.
	 * @exception ComponentNotFoundException if the page is not found
	 */
	public final Page getPage() {
		init();
		return _page;
	}
	private void init() {
		if (_page == null && _uuid != null) {
			_comp = _desktop.getComponentByUuidIfAny(_uuid);
			if (_comp != null) {
				_page = _comp.getPage();
			} else if (!ComponentsCtrl.isUuid(_uuid)) {
				_page = _desktop.getPage(_uuid);
			} else {
				throw new ComponentNotFoundException("Component not found: "+_uuid);
			}
		}
	}
	/** Returns the component that this request is applied for, or null
	 * if it applies to the whole page or a general request.
	 * @exception ComponentNotFoundException if the component is not found
	 */
	public final Component getComponent() {
		init();
		return _comp;
	}
	/** Returns the UUID.
	 */
	public final String getComponentUuid() {
		return _uuid != null ? _uuid: _comp != null ? _comp.getUuid(): null;
	}
	/** Returns the command; never null.
	 */
	public final Command getCommand() {
		return _cmd;
	}
	/** Returns the data of the command, might be null.
	 */
	public final String[] getData() {
		return _data;
	}

	//-- Object --//
	public final boolean equals(Object o) { //prevent override
		return this == o;
	}
	public String toString() {
		if (_uuid != null)
			return "[uuid="+_uuid+", cmd="+_cmd+']';
		else if (_comp != null)
			return "[comp="+_comp+", cmd="+_cmd+']';
		else
			return "[page="+_page+", cmd="+_cmd+']';
	}

	//-- predefined commands --//
	static {
		new BookmarkChangedCommand(Events.ON_BOOKMARK_CHANGED,
			Command.IGNORE_OLD_EQUIV);
		new CheckCommand(Events.ON_CHECK, 0);
		new ClientInfoCommand(Events.ON_CLIENT_INFO, 0);
		new UpdateResultCommand("updateResult", 0);
		new DropCommand(Events.ON_DROP, 0);
		new DummyCommand("dummy", Command.SKIP_IF_EVER_ERROR);
		new ErrorCommand(Events.ON_ERROR, Command.IGNORE_OLD_EQUIV);

		new GenericCommand(Events.ON_BLUR, Command.IGNORE_OLD_EQUIV);
		new GenericCommand(Events.ON_CLOSE, 0);
		new GenericCommand(Events.ON_FOCUS, Command.IGNORE_OLD_EQUIV);
		new GenericCommand(Events.ON_NOTIFY, 0);
		new GenericCommand(Events.ON_SORT,
			Command.SKIP_IF_EVER_ERROR|Command.IGNORE_OLD_EQUIV);
		new GenericCommand(Events.ON_TIMER, Command.IGNORE_OLD_EQUIV);
		new GenericCommand(Events.ON_USER, 0);

		new GetUploadInfoCommand("getUploadInfo", Command.IGNORABLE);

		new InputCommand(Events.ON_CHANGE, Command.IGNORE_IMMEDIATE_OLD_EQUIV);
		new InputCommand(Events.ON_CHANGING,
			Command.SKIP_IF_EVER_ERROR|Command.IGNORABLE);

		new KeyCommand(Events.ON_CANCEL,
			Command.SKIP_IF_EVER_ERROR|Command.CTRL_GROUP);
		new KeyCommand(Events.ON_CTRL_KEY,
			Command.SKIP_IF_EVER_ERROR|Command.CTRL_GROUP);
		new KeyCommand(Events.ON_OK,
			Command.SKIP_IF_EVER_ERROR|Command.CTRL_GROUP);

		new MoveCommand(Events.ON_MOVE, Command.IGNORE_OLD_EQUIV);

		new MouseCommand(Events.ON_CLICK,
			Command.SKIP_IF_EVER_ERROR|Command.CTRL_GROUP);
		new MouseCommand(Events.ON_DOUBLE_CLICK,
			Command.SKIP_IF_EVER_ERROR|Command.CTRL_GROUP);
		new MouseCommand(Events.ON_RIGHT_CLICK,
			Command.SKIP_IF_EVER_ERROR|Command.CTRL_GROUP);

		new OpenCommand(Events.ON_OPEN, 0);
		new RemoveCommand("remove", 0);
		new RedrawCommand("redraw", 0);
		new RemoveDesktopCommand("rmDesktop", 0);
		new RenderCommand(Events.ON_RENDER, Command.IGNORE_OLD_EQUIV);
			//z:loaded is set only if replied from server, so it is OK
			//to drop if any follows -- which means users are scrolling fast

		new ScrollCommand(Events.ON_SCROLLING,
			Command.SKIP_IF_EVER_ERROR|Command.IGNORABLE);
		new ScrollCommand(Events.ON_SCROLL, Command.IGNORE_IMMEDIATE_OLD_EQUIV);

		new SelectCommand(Events.ON_SELECT, 0);
		new ShowCommand(Events.ON_SHOW, 0);

		new ZIndexCommand(Events.ON_Z_INDEX, Command.IGNORE_OLD_EQUIV);
	}
}
