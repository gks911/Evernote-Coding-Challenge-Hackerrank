Evernote-Coding-Challenge-Hackerrank
====================================

Solution to the Evernote Coding challenge that was held on Hackerrank
Input Format 
Your solution must support 4 commands:

`CREATE`

`UPDATE`

`DELETE`

`SEARCH`

The `CREATE`, `UPDATE`, and `DELETE` commands indicate modifications to the set of Notes that can be searched.

### CREATE

The `CREATE` command indicates that a Note should be added to the set of searchable Notes. It will be followed by an XML document with the following format:

```
<note>
  <guid>C93939E4-A607-4EF7-80C1-A1D732C78C87</guid>
  <created>2014-04-16T14:23:11Z</created>
  <tag>armadillo</tag>
  <tag>bonobo</tag>
  <tag>cormorant</tag>
  <content>
    Let us get things started with a coarse-grained overview of the physical makeup of
    the Evernote service. I will not go into a lot of detail on each component here;
    we will aim to talk about the interesting bits in separate posts later.

    Starting at the top-left corner of the diagram, all stats as of May 17th, 2011...

    Networking: Virtually all traffic to and from Evernote comes to www.evernote.com
    via HTTPS port 443. This includes all "web" activity, but also all client
    synchronization via our Thrift-based service API. Altogether, that produces up to
    150 million HTTPS requests per day, with peak traffic around 250
    Mbps. (Unfortunately for our semi-nocturnal Operations team, this daily peak tends
    to arrive around 6:30 am, Pacific time.)
    We use BGP to direct traffic through fully independent network feeds from our
    primary (NTT) and secondary (Level 3) providers. This is filtered through Vyatta
    on the way to the new A10 load balancers that we deployed in January when we hit
    the limit of SSL performance for our old balancers. We are comfortably handling
    existing traffic using one AX 2500 plus a failover box, but we are preparing to
    test their N+1 configuration in our staging cluster to prepare for future growth.
  </content>
</note> 
```
The guid element contains the unique identifier for the Note. This will be used later to update the Note, delete the Note, and indicate that the Note matches a search.

The created element contains an ISO 8601 timestamp in the format yyyy-MM-ddThh:mm:ssZ. The 'Z' indicates that the timestamp is in absolute UTC.

Zero or more tag elements indicate the names of Tags assigned to the Note. Tag names will not include the ':' character.

The content element contains the plain-text content of the Note. The content will consist entirely of ASCII characters. Words containing the "'" character do not need to be normalized.

The closing will be on a line by itself.

### UPDATE

The UPDATE command is used to indicate that a Note changed and will be followed by an XML document with the same format as documented above, with a guid that corresponds to a Note previously added via the CREATE command.

### DELETE

The DELETE command is used to indicate that a Note no longer needs be searchable. It will be followed by a single line containing the GUID of the Note to remove.

### SEARCH

The SEARCH command will be followed by a single line containing a search query.

Your solution must support the following search functions:

Text content (exact and prefix matches) search.

potato matches "Sweet Potato Pie" does not match "Mash four potatoes together"

pot* matches both "Pot: Kettle; Kettle: Pot" and "Sweet Potato Pie"

we're matches "We're going to the circus!"

Tag matches (with the "tag:" operator).

tag:potato matches the tag "potato"

tag:pot* matches the tags "pot" and "potato"

Note creation date/time

created:20140101 matches notes created on or after January 1, 2014. Date boundaries are based on the current local time.

### Output Format

The CREATE, UPDATE, and DELETE commands should produce no output. The SEARCH command should output a single line consisting of a comma separated list of Note GUIDs that match the search. A note matches a search if it matches all of the provided search terms. The results should be ordered by Note creation time (earliest first). If no Notes match the search an empty line should be output.

### Constraints

The input fill will not be greater than 200,000 lines and the entire data can fit in memory.
