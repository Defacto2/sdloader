package sdloader.command;

public interface Command {

	public static final Command STOP = new Command() {
		
		public String toString() {
			return "stop";
		}
	};
}