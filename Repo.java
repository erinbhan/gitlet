package gitlet;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;



/**Repo class for Gitlet, the tiny stupid version-control system.
 *
 * @author Erin Bhan
 */
public class Repo {

    /** The current working dir file.*/
    static final File CWD = new File(System.getProperty("user.dir"));
    /** The gitlet file.*/
    static final File GITLET = new File("./.gitlet");
    /** The commit file.*/
    static final File COMMITS = new File(GITLET, "commits");
    /** The blobs file.*/
    static final File BLOBS = Utils.join(GITLET, "blobs");
    /** The branches file.*/
    static final File BRANCHES = Utils.join(GITLET, "branches");
    /** The globalLog file.*/
    static final File GLOBALLOG = Utils.join(GITLET, "global-log");
    /** The active Branch file.*/
    static final File ACTIVEBRANCH = Utils.join(GITLET, "activeBranch.txt");
    /** The staging file.*/
    static final File STAGINGFILE = Utils.join(GITLET, "stagings.txt");

    /** The Repo file.*/
    public Repo() throws IOException {
        LinkedHashMap<String, String> blobsMap;
    }

    /**
     * Creates a new Gitlet version-control system in the current directory.
     * This system will automatically start with one commit: a commit that
     * contains no files and has the commit message initial commit
     * (just like that,with no punctuation). It will have a single
     * branch: master, which initially
     * points to this initial commit, and master will be the
     * current branch. The timestamp
     * for this initial commit will be 00:00:00 UTC, Thursday, 1 January
     * 1970 in whatever format
     * you choose for dates (this is called "The (Unix) Epoch", represented
     * internally by the time 0.)
     *Since the initial commit in all repositories created by Gitlet will have
     * exactly the same content,
     * it follows that all repositories will automatically share this
     * commit (they will all have the same UID)
     * and all commits in all repositories will trace back to it.
     */
    public void init() throws IOException {
        if (GITLET.exists()) {
            String mess = "A Gitlet version-control system already ";
            System.out.println(mess + "exists in the current directory.");
            return;
        } else {
            GITLET.mkdir();
            COMMITS.mkdir();
            BLOBS.mkdir();
            BRANCHES.mkdir();
            GLOBALLOG.mkdir();
            StagingArea staging = new StagingArea();
            STAGINGFILE.createNewFile();
            Utils.writeObject(STAGINGFILE, staging);
        }

        Commit initial = new Commit("initial commit", null, null);
        String shaCode = Utils.sha1(Utils.serialize(initial));
        File commit = Utils.join(COMMITS, shaCode + ".txt");
        Utils.writeObject(commit, initial);
        File branch = Utils.join(BRANCHES, "master.txt");
        File activeBranch = Utils.join(GITLET, "activeBranch.txt");
        branch.createNewFile();
        activeBranch.createNewFile();
        Utils.writeContents(branch, shaCode);
        Utils.writeContents(activeBranch, "master");

    }

    /**
     * Adds a copy of the file as it currently exists to the staging area
     * (see the description of the commit command). For this reason, adding
     * a file is also called staging the file for addition.
     * Staging an already-staged
     * file overwrites the previous entry in the staging area
     * with the new contents.
     * The staging area should be somewhere in .gitlet. If the
     * current working version
     * of the file is identical to the version in the current
     * commit, do not stage it to be added,
     * and remove it from the staging area if it is already there
     * (as can happen when a file is changed,
     * added, and then changed back). The file will no longer be
     * staged for removal (see gitlet rm),
     * if it was at the time of the command.
     * @param fileName This is file name.
     */
    public void add(String fileName) throws IOException {
        File addFile = Utils.join(CWD, fileName);
        if (!addFile.exists()) {
            System.out.println("File does not exist.");
        } else {
            String activeBranchName = Utils.readContentsAsString(ACTIVEBRANCH);
            File active = Utils.join(BRANCHES, activeBranchName + ".txt");
            String headName = Utils.readContentsAsString(active);
            File headFile = Utils.join(COMMITS, headName + ".txt");
            Commit head = Utils.readObject(headFile, Commit.class);
            String blob = Utils.readContentsAsString(addFile);
            String bSha = Utils.sha1(blob) + Utils.sha1(fileName);
            File blobFile = Utils.join(BLOBS, bSha + ".txt");
            StagingArea stag = Utils.readObject(STAGINGFILE, StagingArea.class);
            LinkedHashMap<String, String> headHash = head.getHashMap();
            if (headHash != null && bSha.equals(headHash.get(fileName))) {
                stag.removeAdd(fileName);
            } else {
                if (!blobFile.exists()) {
                    Utils.writeContents(blobFile, blob);
                }
                stag.add(fileName, bSha);
            }
            stag.removeRemove(fileName);
            Utils.writeObject(STAGINGFILE, stag);
        }
    }

    /**
     * Saves a snapshot of tracked files in the current commit
     * and staging area so they can be restored at a later time,
     * creating a new commit. The commit is said to be tracking
     * the saved files.
     * By default, each commit's snapshot of files will be
     * exactly the same as its parent
     * commit's snapshot of files; it will keep versions of
     * files exactly as they are, and not
     * update them. A commit will only update the contents
     * of files it is tracking that have been
     * staged for addition at the time of commit, in which
     * case the commit will now include the version
     * of the file that was staged instead of the version
     * it got from its parent. A commit will save and
     * start tracking any files that were staged for addition
     * but weren't tracked by its parent. Finally,
     * files tracked in the current commit may be untracked
     * in the new commit as a result being staged for
     * removal by the rm command (below)
     * @param message This is message.
     */
    public void commit(String message) throws IOException {
        StagingArea staging = Utils.readObject(STAGINGFILE, StagingArea.class);
        if (message.length() < 1) {
            System.out.println("Please enter a commit message.");
        }
        LinkedHashMap<String, String> stageAdd = staging.getAddFiles();
        LinkedHashMap<String, String> stageRem = staging.getRemoveFiles();
        if (stageAdd.isEmpty() && stageRem.isEmpty()) {
            System.out.println("No changes added to the commit.");
        } else {
            String activeBranchName = Utils.readContentsAsString(ACTIVEBRANCH);
            File active = Utils.join(BRANCHES, activeBranchName + ".txt");
            String parentName = Utils.readContentsAsString(active);
            File parentFile = Utils.join(COMMITS, parentName + ".txt");
            Commit parent = Utils.readObject(parentFile, Commit.class);
            Commit curr = new Commit(message, parentName, null);
            curr.combine(parent);
            curr.editHash(staging);
            String shaCode1 = Utils.sha1(Utils.serialize(curr));
            File commit = Utils.join(COMMITS, shaCode1 + ".txt");
            commit.createNewFile();
            Utils.writeObject(commit, curr);
            Utils.writeContents(active, shaCode1);
            staging.clear();
            Utils.writeObject(STAGINGFILE, staging);
        }
    }

    /**
     * Unstage the file if it is currently staged for addition.
     * If the file is tracked in the current commit, stage it for
     * removal and remove the file from the working directory if the
     * user has not already done so (do not remove it unless it
     * is tracked in the current commit).
     * @param fileName This is fileNmae.
     */
    public void remove(String fileName) {
        File rmFile = Utils.join(CWD, fileName);
        String activeBranchName = Utils.readContentsAsString(ACTIVEBRANCH);
        File active = Utils.join(BRANCHES, activeBranchName + ".txt");
        String headName = Utils.readContentsAsString(active);
        File headFile = Utils.join(COMMITS, headName + ".txt");
        Commit head = Utils.readObject(headFile, Commit.class);
        StagingArea staging = Utils.readObject(STAGINGFILE, StagingArea.class);
        LinkedHashMap<String, String> sAdd = staging.getAddFiles();
        LinkedHashMap<String, String> headHash = head.getHashMap();
        if (!headHash.containsKey(fileName) && !sAdd.containsKey(fileName)) {
            System.out.print("No reason to remove the file.");
            return;
        } else {
            if (!rmFile.exists()) {
                staging.remove(fileName);
                Utils.writeObject(STAGINGFILE, staging);
            } else {
                String blobSha = Utils.sha1(Utils.readContents(rmFile));
                staging.removeAdd(fileName);
                if (head.getHashMap().containsKey(fileName)) {
                    staging.remove(fileName, blobSha);
                    Utils.restrictedDelete(fileName);
                }
                Utils.writeObject(STAGINGFILE, staging);

            }
        }
    }

    /**
     * Starting at the current head commit, display information
     * about each commit backwards
     * along the commit tree until the initial commit,
     * following the first parent commit links,
     * ignoring any second parents found in merge commits.
     * (In regular Git, this is what you get
     * with git log --first-parent). This set of commit nodes
     * is called the commit's history. For
     * every node in this history, the information it should
     * display is the commit id, the time the
     * commit was made, and the commit message. Here is an
     * example of the exact format it should follow:
     */
    public void log() {
        String activeBranchName = Utils.readContentsAsString(ACTIVEBRANCH);
        File active = Utils.join(BRANCHES, activeBranchName + ".txt");
        String commitName = Utils.readContentsAsString(active);
        while (commitName != null) {
            File comFile = Utils.join(COMMITS, commitName + ".txt");
            Commit curr = Utils.readObject(comFile, Commit.class);
            System.out.println("===");
            System.out.println("commit " + Utils.sha1(Utils.serialize(curr)));
            String m = "Merge: ";
            if (curr.getParent2() != null) {
                String gP2 = curr.getParent2().substring(0, 7);
                String gP1 = curr.getParent().substring(0, 7);
                System.out.println(m + gP1 + " " + gP2);
            }
            Date date = curr.getTimestamp();
            String ptn = "EEE MMM dd kk:mm:ss yyyy ZZZZZ";
            SimpleDateFormat dateFormat = new SimpleDateFormat(ptn);
            String frm = dateFormat.format(date);
            System.out.println("Date: " + frm);
            System.out.println(curr.getMessage());
            System.out.println();
            commitName = curr.getParent();
        }
    }

    /**  user has not already done so (do not remove it unless it
     * is tracked in the current commit).
     * @param fileName This is fileNmae.
     */
    public void checkout1(String fileName) throws IOException {
        String activeBranchName = Utils.readContentsAsString(ACTIVEBRANCH);
        File active = Utils.join(BRANCHES, activeBranchName + ".txt");
        if (!active.exists()) {
            System.out.println("No such branch exists.");
            return;
        }
        String headName = Utils.readContentsAsString(active);
        File head = Utils.join(COMMITS, headName + ".txt");
        Commit com = Utils.readObject(head, Commit.class);
        LinkedHashMap<String, String> hash = com.getHashMap();
        String blobFileName = hash.get(fileName);
        File blobFile = Utils.join(BLOBS, blobFileName + ".txt");
        File f = Utils.join(CWD, fileName);
        if (!f.exists()) {
            f.createNewFile();
        }
        String blobContents = Utils.readContentsAsString(blobFile);
        Utils.writeContents(f, blobContents);
    }

    /**
     * Takes the version of the file as it exists in the commit
     * with the given id, and puts it in the working directory,
     * overwriting the version of the file that's already there
     * if there is one. The new version of the file is not staged.
     * @param fileName This is file name.
     * @param commitID This is commit id.
     */
    public void checkout2(String commitID, String fileName) throws IOException {
        String activeBranchName = Utils.readContentsAsString(ACTIVEBRANCH);
        File active = Utils.join(BRANCHES, activeBranchName + ".txt");
        String headName = Utils.readContentsAsString(active);
        File headFile = Utils.join(COMMITS, headName + ".txt");
        Commit head = Utils.readObject(headFile, Commit.class);
        if (!head.getHashMap().containsKey(fileName)) {
            System.out.print("File does not exist in that commit.");
            return;
        }
        List<String> commitFiles = Utils.plainFilenamesIn(COMMITS);
        for (String s : commitFiles) {
            if (s.contains(commitID)) {
                commitID = s;
            }
        }
        commitID = commitID.substring(0, commitID.length() - 4);
        File commitFile = Utils.join(COMMITS, commitID + ".txt");
        if (!commitFile.exists()) {
            System.out.print("No commit with that id exists.");
            return;
        }
        Commit comOb = Utils.readObject(commitFile, Commit.class);
        LinkedHashMap<String, String> hash = comOb.getHashMap();
        String blobFileName = hash.get(fileName);
        File blobFile = Utils.join(BLOBS, blobFileName + ".txt");
        File f = Utils.join(CWD, fileName);
        if (!f.exists()) {
            f.createNewFile();
        }
        Utils.writeContents(f, Utils.readContents(blobFile));
    }

    /**
     * Takes all files in the commit at the head of the given branch,
     * and puts them in the working directory, overwriting the versions
     * of the files that are already there if they exist. Also, at the end
     * of this command, the given branch will now be considered the current
     * branch (HEAD). Any files that are tracked in the current branch but
     * are not present in the checked-out branch are deleted. The staging
     * area is cleared, unless the checked-out branch is the current branch
     * @param branchName This is branch name.
     */
    public void checkout3(String branchName) throws IOException {
        String activeBranchName = Utils.readContentsAsString(ACTIVEBRANCH);
        if (activeBranchName.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        File branch = Utils.join(BRANCHES, branchName + ".txt");
        if (!branch.exists()) {
            System.out.println("No such branch exists.");
            return;
        }
        File active = Utils.join(BRANCHES, activeBranchName + ".txt");
        String headName = Utils.readContentsAsString(active);
        File headFile = Utils.join(COMMITS, headName + ".txt");
        Commit head = Utils.readObject(headFile, Commit.class);
        String commitName = Utils.readContentsAsString(branch);
        File commit = Utils.join(COMMITS, commitName + ".txt");
        Commit com = Utils.readObject(commit, Commit.class);
        StagingArea staging = Utils.readObject(STAGINGFILE, StagingArea.class);

        for (File file : CWD.listFiles()) {
            LinkedHashMap<String, String> hHash = head.getHashMap();
            LinkedHashMap<String, String> cHash = com.getHashMap();
            String n = file.getName();
            if (!hHash.containsKey(n) && cHash.containsKey(n)) {
                System.out.print("There is an untracked file in the "
                        + "way; delete it, or add and commit it first.");
            }
        }

        for (String fileName : com.getHashMap().keySet()) {
            File blobFile = Utils.join(BLOBS,
                    com.getHashMap().get(fileName) + ".txt");
            File cwdFile = Utils.join(CWD, fileName);
            if (!cwdFile.exists()) {
                cwdFile.createNewFile();
            }
            String blobString = Utils.readContentsAsString(blobFile);
            Utils.writeContents(cwdFile, blobString);
        }
        for (File file : CWD.listFiles()) {
            if (head.getHashMap().containsKey(file.getName())
                    && !com.getHashMap().containsKey(file.getName())) {
                Utils.restrictedDelete(file);
            }
        }
        Utils.writeContents(ACTIVEBRANCH, branchName);
        staging.clear();
        Utils.writeObject(STAGINGFILE, staging);
    }

    /**
     * Like log, except displays information about all commits ever
     * made. The order of the commits does not matter. Hint: there
     * is a useful method in gitlet.Utils that will help you iterate
     * over files within a directory.
     */
    public void globalLog() {
        List<String> glog = Utils.plainFilenamesIn(COMMITS);
        for (String s : glog) {
            File commit = Utils.join(COMMITS, s);
            Commit com = Utils.readObject(commit, Commit.class);
            System.out.println("===");
            System.out.println("commit " + Utils.sha1(Utils.serialize(com)));
            Date date = com.getTimestamp();
            String ptn = "EEE MMM dd kk:mm:ss yyyy ZZZZZ";
            SimpleDateFormat dateFormat = new SimpleDateFormat(ptn);
            String frm = dateFormat.format(date);
            System.out.println("Date: " + frm);
            System.out.println(com.getMessage());
            System.out.println();
        }
    }

    /**
     * Prints out the ids of all commits that have the given commit message,
     * one per line. If there are multiple such commits, it prints the ids
     * out on separate lines. The commit message is a single operand; to
     * indicate a multiword message, put the operand in quotation marks,
     * as for the commit command above.
     * @param msg This is message.
     */
    public void find(String msg) {
        List<String> glog = Utils.plainFilenamesIn(COMMITS);
        boolean commitE = false;
        for (String s : glog) {
            File commit = Utils.join(COMMITS, s);
            Commit com = Utils.readObject(commit, Commit.class);
            if (com.getMessage().equals(msg)) {
                commitE = true;
                System.out.println(Utils.sha1(Utils.serialize(com)));
            }
        }
        if (!commitE) {
            System.out.println("Found no commit with that message");
            return;
        }
    }

    /**
     * Displays what branches currently exist, and marks the
     * current branch with a *.
     * Also displays what files have been staged for addition
     * or removal. An example
     * of the exact format it should follow is as follows.
     */
    public void status() {
        System.out.println("=== Branches ===");
        String activeBranchName = Utils.readContentsAsString(ACTIVEBRANCH);
        String branchName = "";
        System.out.println("*" + activeBranchName);
        for (File branch : BRANCHES.listFiles()) {
            branchName = branch.getName();
            branchName = branchName.substring(0, branchName.length() - 4);
            if (!activeBranchName.equals(branchName)) {
                System.out.println(branchName);
            }
        }
        System.out.println();
        StagingArea staging = Utils.readObject(STAGINGFILE, StagingArea.class);
        System.out.println("=== Staged Files ===");
        if (!(staging == null) && !staging.getAddFiles().isEmpty()) {
            for (String st : staging.getAddFiles().keySet()) {
                System.out.println(st);
            }
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        if (!(staging == null) && !staging.getRemoveFiles().isEmpty()) {
            for (String st : staging.getRemoveFiles().keySet()) {
                System.out.println(st);
            }
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
//        File active = Utils.join(BRANCHES, activeBranchName + ".txt");
//        String headName = Utils.readContentsAsString(active);
//        File headFile = Utils.join(COMMITS, headName + ".txt");
//        Commit head = Utils.readObject(headFile, Commit.class);
//        HashMap<String, String> headF = head.getHashMap();
//        String sha, remF, bl = "";
//        boolean h, st;
//        for (File file : CWD.listFiles()) {
//            if (file.isFile()) {
//                sha= Utils.sha1(Utils.readContentsAsString(file));
//                remF = file.getName();
//                h = headF.containsKey(remF);
//                st = staging.getAddFiles().containsValue(sha) || staging.getRemoveFiles().containsValue(sha);
//                if (h && !st) {
//                    for (File blobF : BLOBS.listFiles()) {
//                        bl = blobF.getName();
//                        bl = bl.substring(0, bl.length() - 4);
//                        if (bl.equals(sha)) {
//                            break;
//                        }
//                    }
//                    if(!bl.equals(sha)) {
//                        System.out.println(file.getName() + " (modified)");
//                    }
//                }
//            }
//        }
//        String fileS = "";
//        for (String s : headF.keySet()) {
//            for (File f : CWD.listFiles()) {
//                fileS = f.getName();
//                if (s.equals(fileS)) {
//                    break;
//                }
//            }
//            if (!s.equals(fileS) && !staging.getRemoveFiles().containsKey(s)) {
//                System.out.println(s + " (deleted)");
//            }
//        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
//        for (File f : CWD.listFiles()) {
//            if (f.isFile()) {
//                remF = f.getName();
//                h = headF.containsKey(remF);
//                st = staging.getAddFiles().containsValue(remF) || staging.getRemoveFiles().containsValue(remF);
//                if (!h && !st) {
//                    System.out.println(remF);
//                }
//            }
      //  }
    }

    /**
     * Creates a new branch with the given name, and points it at the
     * current head node.
     * A branch is nothing more than a name for a reference (a SHA-1
     * identifier) to a
     * commit node. This command does NOT immediately switch to the
     * newly created branch
     * (just as in real Git). Before you ever call branch, your code
     * should be running with
     * a default branch called "master".
     * @param branchName This is branchName.
     */
    public void branch(String branchName) throws IOException {
        File branch = Utils.join(BRANCHES, branchName + ".txt");
        if (branch.exists()) {
            System.out.println("A branch with that name already exists.");
            return;
        } else {
            branch.createNewFile();
            String activeBranchName = Utils.readContentsAsString(ACTIVEBRANCH);
            File branchFile = Utils.join(BRANCHES, activeBranchName + ".txt");
            Utils.writeContents(branch, Utils.readContentsAsString(branchFile));
        }
    }

    /**
     * Deletes the branch with the given name. This only means to
     * delete the pointer
     * associated with the branch; it does not mean to delete all
     * commits that were
     * created under the branch, or anything like that.
     * @param branchName This is branch name.
     */
    public void rmBranch(String branchName) {
        File branch = Utils.join(BRANCHES, branchName + ".txt");
        if (!branch.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        String activeBranchName = Utils.readContentsAsString(ACTIVEBRANCH);
        if (activeBranchName.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        branch.delete();
    }

    /**
     * Checks out all the files tracked by the given commit. Removes tracked
     * files that are not present in that commit. Also moves the current
     * branch's head to that commit node. See the intro for an example of what
     * happens to the head pointer after using reset. The [commit id] may be
     * abbreviated as for checkout. The staging area is cleared. The command is
     * essentially checkout of an arbitrary commit that also changes the
     * current branch head.
     * @param commitID This is commit id.
     */
    public void reset(String commitID) throws IOException {
        String activeBranchName = Utils.readContentsAsString(ACTIVEBRANCH);
        File active = Utils.join(BRANCHES, activeBranchName + ".txt");
        String headName = Utils.readContentsAsString(active);
        File headFile = Utils.join(COMMITS, headName + ".txt");
        Commit head = Utils.readObject(headFile, Commit.class);
        List<String> commitFiles = Utils.plainFilenamesIn(COMMITS);
        for (String s : commitFiles) {
            if (s.contains(commitID)) {
                commitID = s;
            }
        }
        commitID = commitID.substring(0, commitID.length() - 4);
        File commit = Utils.join(COMMITS, commitID + ".txt");
        if (!commit.exists()) {
            System.out.print("No commit with that id exists.");
            return;
        }
        Commit com = Utils.readObject(commit, Commit.class);
        StagingArea staging = Utils.readObject(STAGINGFILE, StagingArea.class);

        for (File file : CWD.listFiles()) {
            if (!head.getHashMap().containsKey(file.getName())
                    && com.getHashMap().containsKey(file.getName())) {

                System.out.print("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }
        }
        for (String fileName : com.getHashMap().keySet()) {
            File blobFile = Utils.join(BLOBS,
                    com.getHashMap().get(fileName) + ".txt");
            File cwdFile = Utils.join(CWD, fileName);

            if (!cwdFile.exists()) {
                cwdFile.createNewFile();
            }
            String blobString = Utils.readContentsAsString(blobFile);
            Utils.writeContents(cwdFile, blobString);
        }
        for (File file : CWD.listFiles()) {
            if (head.getHashMap().containsKey(file.getName())
                    && !com.getHashMap().containsKey(file.getName())) {
                Utils.restrictedDelete(file);
            }
        }
        Utils.writeContents(active, commitID);
        staging.clear();
        Utils.writeObject(STAGINGFILE, staging);
    }

    /**
     * branch1 = headName.
     * branch2 = branchName The one were merging.
     * @param branchName This is branch name.
     */
    public void mergeT(String branchName) throws IOException {
        String activeBranchName = Utils.readContentsAsString(ACTIVEBRANCH);
        File active = Utils.join(BRANCHES, activeBranchName + ".txt");
        String headName = Utils.readContentsAsString(active);
        File headFile = Utils.join(COMMITS, headName + ".txt");
        Commit head = Utils.readObject(headFile, Commit.class);
        File branch = Utils.join(BRANCHES, branchName + ".txt");
        String commitName = Utils.readContentsAsString(branch);
        String splitPlaceSHA1 = splitSection(branchName);
        Commit splitPlace = getCommit(splitPlaceSHA1);
        if (splitPlaceSHA1.equals(commitName)) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
            return;
        }
        if (splitPlace.getTimestamp().equals(head.getTimestamp())) {
            System.out.println("Current branch fast-forwarded.");
            checkout3(branchName);
            return;
        }
    }
    /**
     * branch1 = headName.
     * branch2 = branchName (the one were merging.
     * @param branchName This is branch name.
     */
    public void merge(String branchName) throws IOException {
        String activeBranchName = Utils.readContentsAsString(ACTIVEBRANCH);
        File active = Utils.join(BRANCHES, activeBranchName + ".txt");
        String headName = Utils.readContentsAsString(active);
        File headFile = Utils.join(COMMITS, headName + ".txt");
        Commit head = Utils.readObject(headFile, Commit.class);
        File branch = Utils.join(BRANCHES, branchName + ".txt");
        if (!branch.exists()) {
            System.out.println("A branch with that name does not exists.");
            return;
        }
        String commitName = Utils.readContentsAsString(branch);
        File commit = Utils.join(COMMITS, commitName + ".txt");
        Commit com = Utils.readObject(commit, Commit.class);
        StagingArea st = Utils.readObject(STAGINGFILE, StagingArea.class);
        if (activeBranchName.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        String splitPlaceSHA1 = splitSection(branchName);
        Commit splitPlace = getCommit(splitPlaceSHA1);
        if (st.getAddFiles().size() != 0 || st.getRemoveFiles().size() != 0) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        List<String> cwdFiles = Utils.plainFilenamesIn(CWD);
        for (String file : cwdFiles) {
            if (!head.getHashMap().containsKey(file)) {
                System.out.println("There is an untracked file in "
                        + "the way; delete it, or add and commit it first.");
                return;
            }
        }
        mergeT(branchName);
        HashSet<String> bSet = new HashSet<>(splitPlace.getHashMap().keySet());
        bSet.addAll(com.getHashMap().keySet());
        bSet.addAll(head.getHashMap().keySet());
        boolean mergeBool = false;
        for (String s : bSet) {
            boolean cH = com.getHashMap().containsKey(s);
            boolean sH = splitPlace.getHashMap().containsKey(s);
            if (head.getHashMap().containsKey(s) && cH && sH) {
                m1(s, head, com, splitPlace, commitName);
                mergeBool = merge2(s, head, com, splitPlace) || mergeBool;
            } else if (!head.getHashMap().containsKey(s) && cH && !sH) {
                merge3(s, com);
            } else if (head.getHashMap().containsKey(s) && !cH && sH) {
                mergeBool = merge2(s, head, com, splitPlace) || mergeBool;
                merge4(s, head, splitPlace);
            } else if (!head.getHashMap().containsKey(s) && cH && sH) {
                mergeBool = merge2(s, head, com, splitPlace) || mergeBool;
            } else if (head.getHashMap().containsKey(s) && cH && !sH) {
                mergeBool = merge2(s, head, com, splitPlace) || mergeBool;
            }
        }
        commitMerge(branchName, headName, commitName);
        if (mergeBool) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** HASHset.
     * @param otherBranchName This is currsha.
     * @param headName1 This is headName.
     * @param commitName This is commit name.
     */
    private static void commitMerge(String otherBranchName,
                                    String headName1, String commitName) {
        StagingArea staging = Utils.readObject(STAGINGFILE, StagingArea.class);
        String activeBranchName = Utils.readContentsAsString(ACTIVEBRANCH);
        File active = Utils.join(BRANCHES, activeBranchName + ".txt");
        Commit commit2 = new Commit("Merged "
                + otherBranchName + " into " + activeBranchName
                + ".", null, null);
        String headName = Utils.readContentsAsString(active);
        File headFile = Utils.join(COMMITS, headName + ".txt");
        Commit head = Utils.readObject(headFile, Commit.class);
        commit2.getHashMap().putAll(head.getHashMap());
        commit2.getHashMap().putAll(staging.getAddFiles());
        for (Map.Entry<String,
                String> s : staging.getRemoveFiles().entrySet()) {
            commit2.getHashMap().remove(s);
        }
        commit2.setParent(headName1);
        commit2.setParent2(commitName);
        commit2.setBranch(headName1);
        commit2.setBranch2(commitName);
        byte[] serializedCommit2 = Utils.serialize(commit2);
        String newComSha = Utils.sha1((Object) serializedCommit2);
        File comFile = Utils.join(COMMITS, newComSha + ".txt");
        Utils.writeObject(comFile, commit2);
        Utils.writeContents(active, newComSha);
        staging.clear();
        Utils.writeObject(STAGINGFILE, staging);
    }


    /** HASHset.
     * @param s This is s.
     * @param m This is split.
     * @param c This is com.
     * @param p This is splitstring.
     * @param h This is head.
     */
    public void m1(String s, Commit h, Commit c,
                   Commit m, String p) throws IOException {
        HashMap<String, String> headB = h.getHashMap();
        HashMap<String, String> comB = c.getHashMap();
        HashMap<String, String> splitB = m.getHashMap();
        StagingArea staging = Utils.readObject(STAGINGFILE, StagingArea.class);
        if (!comB.get(s).equals(splitB.get(s))) {
            if (headB.get(s).equals(splitB.get(s))) {
                {
                    checkout2(p, s);
                    staging.add(s);
                    staging.remove(s, comB.get(s));
                }
                Utils.writeObject(STAGINGFILE, staging);
            }
        }
    }

    /** Boolean.
     * @param s This is currsha.
     * @param com This is com.
     * @param split This is split.
     * @param head This is head.
     * @return true or false.*/
    public boolean merge2(String s, Commit head, Commit com, Commit split) {
        HashMap<String, String> hB = head.getHashMap();
        HashMap<String, String> comB = com.getHashMap();
        HashMap<String, String> splitB = split.getHashMap();
        StagingArea staging = Utils.readObject(STAGINGFILE, StagingArea.class);
        boolean bool = false;
        boolean hCon = hB.containsKey(s);
        boolean cContain = comB.containsKey(s);
        String writing = "";
        if (!hB.containsKey(s) && !comB.get(s).equals(splitB.get(s))) {
            File comFile = Utils.join(BLOBS, comB.get(s) + ".txt");
            String comString = Utils.readContentsAsString(comFile);
            writing = "<<<<<<< HEAD\n" + "=======\n" + comString + ">>>>>>>\n";
            bool = true;
        } else if (!comB.containsKey(s) && !hB.get(s).equals(splitB.get(s))) {
            File headFile = Utils.join(BLOBS, hB.get(s) + ".txt");
            String headString = Utils.readContentsAsString(headFile);
            writing = "<<<<<<< HEAD\n" + headString + "=======\n" + ">>>>>>>\n";
            bool = true;
        } else if (hCon && cContain && !hB.get(s).equals(splitB.get(s))) {
            if (!hB.get(s).equals(splitB.get(s))) {
                if (!comB.get(s).equals(splitB.get(s))) {
                    File headFile = Utils.join(BLOBS, hB.get(s) + ".txt");
                    String headStr = Utils.readContentsAsString(headFile);
                    File comFile = Utils.join(BLOBS, comB.get(s) + ".txt");
                    String comString = Utils.readContentsAsString(comFile);
                    String he = "<<<<<<< HEAD\n";
                    String sp = "=======\n";
                    String end = ">>>>>>>\n";
                    writing = he + headStr + sp + comString + end;
                    bool = true;
                }
            }
        }
        if (bool) {
            String blobName = Utils.sha1(writing) + Utils.sha1(s);
            File blobFile = Utils.join(BLOBS, blobName + ".txt");
            Utils.writeObject(blobFile, writing);
            File fileFile = Utils.join(CWD, s);
            Utils.writeContents(fileFile, writing);
            staging.add(s);
        }
        Utils.writeObject(STAGINGFILE, staging);
        return bool;
    }

    /** HASHset.
     * @param s This is s.
     * @param com Returns hist.*/
    private static void merge3(String s, Commit com) {
        HashMap<String, String> comB = com.getHashMap();
        StagingArea staging = Utils.readObject(STAGINGFILE, StagingArea.class);
        File f = Utils.join(CWD, s);
        File blobF = Utils.join(BLOBS, comB.get(s) + ".txt");
        String cont = Utils.readContentsAsString(blobF);
        Utils.writeContents(f, cont);
        staging.add(s, com.getHashMap().get(s));
        Utils.writeObject(STAGINGFILE, staging);
    }
    /** HASHset.
     * @param s This is string.
     * @param head This is head
     * @param split This is split.*/
    private static void merge4(String s, Commit head, Commit split) {
        HashMap<String, String> headB = head.getHashMap();
        HashMap<String, String> splitB = split.getHashMap();
        StagingArea staging = Utils.readObject(STAGINGFILE, StagingArea.class);
        if (headB.get(s).equals(splitB.get(s))) {
            File f = Utils.join(CWD, s);
            f.delete();
            staging.getRemoveFiles().put(s, headB.get(s));
        }
        Utils.writeObject(STAGINGFILE, staging);
    }

    /** Commit.
     * @param code This is code.
     * @return Returns hist.*/
    private static Commit getCommit(String code) {
        File comFile = Utils.join(COMMITS, code + ".txt");
        Commit com = Utils.readObject(comFile, Commit.class);
        return com;
    }
    /** HASHset.
     * @param branchName This is branch name.
     * @return Returns hist.*/
    private static String splitSection(String branchName) {
        List<String> commitLists = Utils.plainFilenamesIn(COMMITS);
        File passingFile = Utils.join(BRANCHES, branchName + ".txt");
        String passingBranchName = Utils.readContentsAsString(passingFile);
        String activeBranchName = Utils.readContentsAsString(ACTIVEBRANCH);
        File active = Utils.join(BRANCHES, activeBranchName + ".txt");
        String headName = Utils.readContentsAsString(active);
        File headFile = Utils.join(COMMITS, headName + ".txt");
        HashSet<String> currComHistory;
        HashSet<String> mergeComHistory;
        ArrayList<String> commAn = new ArrayList<>();
        currComHistory = commitHis(headName);
        mergeComHistory = commitHis(passingBranchName);
        for (String s : currComHistory) {
            if (mergeComHistory.contains(s)) {
                commAn.add(s);
            }
        }
        HashMap<String, Boolean> markingCom = new HashMap<>();
        for (String c : commitLists) {
            markingCom.put(c, false);
        }
        String split = bfs(commAn, headName, markingCom);
        return split;
    }
    /** HASHset.
     * @param currSha1 This is currsha.
     * @return Returns hist.*/
    private static HashSet<String> commitHis(String currSha1) {
        HashSet<String> hist = new HashSet<>();

        if (getCommit(currSha1).getParent() != null) {
            hist.addAll(commitHis(getCommit(currSha1).getParent()));
        }
        hist.add(currSha1);
        if (getCommit(currSha1).getParent2() != null) {
            hist.addAll(commitHis(getCommit(currSha1).getParent2()));
        }
        return hist;
    }

    /** StagingARea class.
     * @param c This is comm.
     * @param m This is the marking.
     * @param s This is shaCode.
     * @return null.*/
    public static String bfs(ArrayList<String> c,
                             String s, HashMap<String, Boolean> m) {
        ArrayDeque<String> deq = new ArrayDeque<>();
        deq.add(s);
        m.put(s, true);
        String commitString = "";
        while (!deq.isEmpty()) {
            String remCom = deq.remove();
            Commit getCom = getCommit(remCom);
            String comPsha = getCom.getParent();
            String comP2sha = getCom.getParent2();
            if (c.contains(remCom)) {
                commitString = remCom;
                return commitString;
            }
            if (!m.containsKey(comPsha)) {
                deq.add(comPsha);
                m.put(comPsha, true);
            }
            if (comP2sha != null && !m.containsKey(comP2sha)) {
                deq.add(comP2sha);
                m.put(comP2sha, true);
            }
        }
        return null;
    }
}
