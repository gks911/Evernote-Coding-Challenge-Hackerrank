import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Gaurav
 * 
 *         Algorithm: Create a Ternary Search Trie (TST) for the words in
 *         <content> and <tags>, separately. Tries are suited for exact word
 *         searches and prefix searches, which was the scope of this challenge.
 *         TSTs, in addition also use comparatively lesser space than tries.
 *         Each node in my trie stores a character from the corpus, and if the
 *         root-to-node path ends in a word, I store the document's guid in that
 *         node as well.
 * 
 *         For search in multiple fields, such as `SEARCH tag:xyz
 *         created:YYYYmmdd abc`, I take the intersection of documents in
 *         progression.
 * 
 *         For Delete, I planned to use a lazy approach, i.e. not cleaning up
 *         the trie after each delete, but rather after a fixed number of
 *         deletions. (kind of like batch deletions). Although, I do remove the
 *         guid from the trie to make the deleted document un-searchable, I am
 *         not cleaning up the trie for the purposes of this contest. It can
 *         simply remove unwanted nodes from the trie and save space.
 * 
 *         For updates, I first remove the existing note, and then re-insert the
 *         new note. Again, saves me from writing some nasty code for deletion
 *         :)
 * 
 *         I have used a regex based XML parser, instead of using a full fledged
 *         XML parser like SAX, since the note structure was simplistic
 * 
 * 
 */
class Solution {
	Map<String, EvernoteObj> notes;
	TernarySearchTrie contentTrie;
	TernarySearchTrie tagTrie;
	Pattern pattern;

	public Solution() {
		notes = new HashMap<String, EvernoteObj>();
		contentTrie = new TernarySearchTrie();
		tagTrie = new TernarySearchTrie();
	}

	public static void main(String[] args) {
		Solution sol = new Solution();
		Scanner scanner = new Scanner(System.in);
		while (scanner.hasNext()) {
			String line = scanner.nextLine();
			if (line.trim().equals("CREATE")) {
				EvernoteObj note=sol.parseNote(scanner);
				sol.createNote(note);
			} else if (line.trim().equals("SEARCH")) {
					String keyword[] = scanner.nextLine().split(" ");
					TreeSet<EvernoteObj> sorted = null;
					if (keyword.length > 1) {
						Set<String> intersection = new HashSet<String>();
						Set<String> _tmp = new HashSet<String>();
						boolean first = true;
						for (String s : keyword) {
							if (first) {
								_tmp.addAll(sol.searchWord(s.toLowerCase()));
								first = false;
							} else {
								Set<String> set = sol.searchWord(s.toLowerCase());
								for (String s1 : set)
									if (_tmp.contains(s1))
										intersection.add(s1);
							}
						}
						sorted = new TreeSet<EvernoteObj>();
						for(String s:intersection)
							sorted.add(sol.notes.get(s));
					} else{
						sorted = new TreeSet<EvernoteObj>();
						for(String s:sol.searchWord(keyword[0].toLowerCase()))
							sorted.add(sol.notes.get(s));
					}
					System.out.println(sorted.toString().replace("[", "")
							.replace("]", "").replaceAll(", ", ",").trim());
			} else if (line.trim().equals("UPDATE")) {
				EvernoteObj note=sol.parseNote(scanner);
				sol.deleteNote(note.guid);
				sol.createNote(note);
			} else if (line.trim().equals("DELETE")) {
				String deleteGuid = scanner.nextLine();
				sol.deleteNote(deleteGuid);
			} else 
				break;
		}
		scanner.close();
	}

	/**
	 * Regex based XML Parser 
	 * @param scanner
	 * @return
	 */
	public EvernoteObj parseNote(Scanner scanner) {
		StringBuilder sb = new StringBuilder();
		String data;
		do {
			data = scanner.nextLine();
			sb.append(data);
		} while (!data.equals("</note>"));
		
		String note = getXmlTag(sb.toString(),"<note>(.+?)</note>");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		Date date=null;
		try {
			date = df.parse(getXmlTag(note, "<created>(.+?)</created>"));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return new EvernoteObj(getXmlTag(note, "<guid>(.+?)</guid>"), date,
				getTags(note), getXmlTag(note, "<content>(.+?)</content>"));
	}
	
	/**
	 * Updates the trie with the contents/tags of the note
	 * @param obj
	 */
	public void updateTries(EvernoteObj obj){
		for (String x : obj.tags)
			tagTrie.insert(x.toLowerCase(), obj);

		for (String x : obj.content.split("[^a-zA-Z0-9\\']")) {
			if (x.trim().length() != 0) 
				contentTrie.insert(x.toLowerCase(), obj);
		}
	}

	/**
	 * Takes in a pattern(for a XML tag), and returns the content inside the tag
	 * 
	 * @param data
	 * @param patt
	 * @return
	 */
	private String getXmlTag(String data, String patt){
		pattern = Pattern.compile(patt);
		Matcher matcher = pattern.matcher(data);
		matcher.find();
		return matcher.group(1);
	}
	
	/**
	 * Parses the <tag>...</tag> field
	 * @param data
	 * @return
	 */
	private Set<String> getTags(String data){
		pattern = Pattern.compile("<tag>(.+?)</tag>", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(data);
		Set<String> tags = new HashSet<String>();
		while(matcher.find())
			tags.add(matcher.group(1));
		return tags;
	}
	
	/**
	 * Creates a new note, and updates internal structures
	 * @param note
	 * @return
	 */
	private EvernoteObj createNote(EvernoteObj note){
		notes.put(note.guid, note);
		updateTries(note);
		return note;
	}
	
	private void deleteNote(String deleteGuid){
		EvernoteObj tmp = notes.get(deleteGuid);
		tmp.deleted = true;

		//delete GUID from the Tries
		tagTrie.deleteFromAllNodes(tagTrie.root, "", tmp.guid);
		contentTrie.deleteFromAllNodes(contentTrie.root, "",
				tmp.guid);
		notes.put(deleteGuid, tmp);
	}
	
	/**
	 * Parses the SEARCH query input, and searches for the notes in the tries
	 * (or the list of notes if `created:..` is used)
	 * 
	 * @param word
	 * @return Set of GUIDs matching the search criteria
	 */
	private Set<String> searchWord(String word) {
		if (word.contains("tag:"))
			return searchTag(word.substring(word.indexOf(':') + 1,
					word.length()));
		else if (word.contains("created:")) {
			// return guids based on date
			DateFormat df = new SimpleDateFormat("yyyyMMdd");
			df.setTimeZone(TimeZone.getTimeZone("UTC"));
			try {
				Date filter = df.parse(word.substring(word.indexOf(':') + 1,
						word.length()));
				Set<String> set = new HashSet<String>();
				for (EvernoteObj e : notes.values()) {
					if (filter.compareTo(e.created) <= 0 && !e.deleted)
						set.add(e.guid);
				}
				return set;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		//Handle a prefix search
		if (word.contains("*")) {
			Set<String> set = new HashSet<String>();
			for (Node x : contentTrie.searchPrefix(word.toLowerCase()
					.substring(0, word.indexOf('*')))) {
				if (x.guids != null)
					set.addAll(x.guids);
			}
			return set;
		} else
			//routine word search in the trie
			return contentTrie.searchword(word.toLowerCase()).guids;
	}

	/**
	 * Helper method to handle a search based on `tag:...`
	 * @param word
	 * @return
	 */
	private Set<String> searchTag(String word) {
		if (word.contains("*")) {
			Set<String> set = new HashSet<String>();
			for (Node x : tagTrie.searchPrefix(word.toLowerCase().substring(0,
					word.indexOf('*')))) {
				if (x.guids != null)
					set.addAll(x.guids);
			}
			return set;
		} else
			return tagTrie.searchword(word.toLowerCase()).guids;
	}
}

/**
 * Class representing a Trie node
 */
class Node {
	Node left, right, mid;
	char c;
	Set<String> guids;

	public Node() {
		guids = new HashSet<String>();
	}
}

/**
 * Class representing an Evernote object. Implements Comparable to sort based on
 * creation dates
 */
class EvernoteObj implements Comparable<EvernoteObj> {
	String guid;
	Date created;
	Set<String> tags;
	String content;
	boolean deleted;

	public EvernoteObj(String guid,Date created,Set<String> tags,String content) {
		this.guid = guid;
		this.created = created;
		this.tags=tags;
		this.content=content;
	}
	@Override
	public int compareTo(EvernoteObj o) {
		return this.created.compareTo(o.created);
	}
	@Override
	public String toString() {
		return guid;
	}
}

/**
 * The crux of the searching algorithm. Implements the TST structure along with
 * methods for insertion, searching and (lazy) deletes
 */
class TernarySearchTrie {
	Node root;
	
	/**
	 * Inserts the given word into the trie
	 * @param word
	 * @param note
	 * @return
	 */
	public Node insert(String word, EvernoteObj note) {
		root = _insert(root, word, note, 0);
		return root;
	}

	private Node _insert(Node x, String word, EvernoteObj note, int i) {
		char c = word.charAt(i);
		if (x == null) {
			x = new Node();
			x.c = c;
		}
		if (c < x.c)
			x.left = _insert(x.left, word, note, i);
		else if (c > x.c)
			x.right = _insert(x.right, word, note, i);
		else if (i < word.length() - 1)
			x.mid = _insert(x.mid, word, note, i + 1);
		else
			// repeated word, add this note's guid to existing set
			x.guids.add(note.guid);
		return x;
	}

	/**
	 * Searches for a given word in the Trie
	 * @param word
	 * @return
	 */
	public Node searchword(String word) {
		Node node = _searchWord(root, word, 0);
		return node;
	}

	private Node _searchWord(Node x, String word, int i) {
		if (x == null)
			return null;
		char c = word.charAt(i);
		if (c < x.c)
			return _searchWord(x.left, word, i);
		else if (c > x.c)
			return _searchWord(x.right, word, i);
		else if (i < word.length() - 1)
			return _searchWord(x.mid, word, i + 1);
		else
			return x;
	}

	/**
	 * Deletes the input note GUID from all the nodes in the trie, making that
	 * note virtually invisible for searches.
	 * 
	 * @param node
	 * @param prefix
	 * @param guid
	 */
	public void deleteFromAllNodes(Node node, String prefix, String guid) {
		if (node == null)
			return;
		deleteFromAllNodes(node.left, prefix, guid);
		if (node.guids.size() != 0 && node.guids.contains(guid))
			node.guids.remove(guid);
		deleteFromAllNodes(node.mid, prefix + node.c, guid);
		deleteFromAllNodes(node.right, prefix, guid);
	}

	/**
	 * Populates a list of Nodes which are a proper word in the corpus (i.e. for
	 * which the nodes have non-empty guid set)
	 * 
	 * @param node
	 * @param prefix
	 * @param nodeList
	 */
	private void getAllNodes(Node node, String prefix, List<Node> nodeList) {
		if (node == null)
			return;
		getAllNodes(node.left, prefix, nodeList);
		if (node.guids.size() != 0)
			nodeList.add(node);
		getAllNodes(node.mid, prefix + node.c, nodeList);
		getAllNodes(node.right, prefix, nodeList);
	}

	/**
	 * Searches the trie based on word prefixes 
	 * @param prefix
	 * @return
	 */
	public List<Node> searchPrefix(String prefix) {
		List<Node> nodeList = new ArrayList<Node>();
		Node node = _searchWord(root, prefix, 0);
		if (node == null)
			return nodeList;
		if (node.guids.size() != 0)
			nodeList.add(node);
		getAllNodes(node.mid, prefix, nodeList);
		return nodeList;
	}
}

