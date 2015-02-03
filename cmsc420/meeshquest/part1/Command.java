package cmsc420.meeshquest.part1;

import org.w3c.dom.Element;

public class Command {

	public String name;
	public Element command;

	public Command(String name, Element command) {
		this.name = name;
		this.command = command;
	}

	public String getCommandType() {
		return this.name;
	}

	public Element getCommandElement() {
		return this.command;
	}

	/**
	 * Gets the attribute from the command by name.
	 * 
	 * @param s
	 * @return empty string is attribute not found
	 */
	public String getAttribute(String s) {
		return this.command.getAttribute(s);
	}

	public String getCommandName() {
		return this.name;
	}

}
