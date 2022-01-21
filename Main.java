package gitlet;

import java.io.IOException;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author Erin Bhan
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */
    public static void main(String... args) throws IOException {
        Repo repo = new Repo();
        if (args.length == 0) {
            exitWithError("Please enter a command.");
        }
        if (!repo.GITLET.exists() && !args[0].equals("init")) {
            exitWithError("Not in an initialized Gitlet directory.");
        }
        switch (args[0]) {
        case "init":
            repo.init(); break;
        case "add":
            repo.add(args[1]); break;
        case "commit":
            repo.commit(args[1]); break;
        case "rm":
            repo.remove(args[1]); break;
        case "log":
            repo.log(); break;
        case "global-log":
            repo.globalLog(); break;
        case "find":
            repo.find(args[1]); break;
        case "status":
            repo.status(); break;
        case "checkout":
            if (args.length == 3 && args[1].equals("--")) {
                repo.checkout1(args[2]);
            } else if (args.length == 4 && args[2].equals("--")) {
                repo.checkout2(args[1], args[3]);
            } else if (args.length == 2) {
                repo.checkout3(args[1]);
            } else {
                System.out.println("Incorrect operands.");
            }
            break;
        case "branch":
            repo.branch(args[1]); break;
        case "rm-branch":
            repo.rmBranch(args[1]); break;
        case "reset":
            repo.reset(args[1]); break;
        case "merge":
            repo.merge(args[1]); break;
        default:
            System.out.print("No command with that name exists.");
        }
        return;
    }

    /** exits with Error.
     * @param message The input message.*/
    public static void exitWithError(String message) {
        if (message != null && !message.equals("")) {
            System.out.print(message);
        }
        System.exit(0);
    }

}
