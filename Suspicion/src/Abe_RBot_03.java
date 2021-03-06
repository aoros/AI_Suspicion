
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

/**
 * This class uses the basic idea of probability to decide the best guess for
 * the suspect reporting at the end of the game. Also added the method to remove
 * guest names from the possible suspects when the "peek at deck" action card is
 * played.
 *
 * @author Abraham Oros
 */
public class Abe_RBot_03 extends Bot {

    Random rand = new Random();
    HashMap<String, Piece> pieces; // Keyed off of guest name
    Board board;
    Piece me;
    HashMap<String, Player> players; // Keyed off of player name
    String otherPlayerNames[];
    private static final boolean PRINT_OUT_ALL_POSSIBLE_GUESSES = false;
    private final String[] allCharacters = {"Buford Barnswallow", "Earl of Volesworthy", "Mildred Wellington", "Nadia Bwalya",
        "Viola Chung", "Dr. Ashraf Najem", "Remy La Rocque", "Lily Nesbit", "Trudie Mudge",
        "Stefano Laconi"};

    private Map<String, String> getGuessBasedOnProbabilities() {
        Map<String, String> plyrToGuessMap = new HashMap<>();

        Collection<List<String>> allPerms = getAllGuessPermutations();
        Collection<List<String>> filteredPerms = removeInvalidPermutations(allPerms);

        int i = 0;
        for (String plyrName : players.keySet()) {
            int lastCount = 0;
            for (String charName : allCharacters) {
                int count = 0;
                for (List<String> perm : filteredPerms) {
                    if (charName.equals(perm.get(i)))
                        count++;
                }
                if (count > lastCount) {
                    plyrToGuessMap.put(plyrName, charName);
                    lastCount = count;
                }
            }
            i++;
        }

        return plyrToGuessMap;
    }

    public class Board {

        public Room rooms[][];

        public class Room {

            public final boolean gems[] = new boolean[3];
            public final String[] availableGems;
            public final int row;
            public final int col;
            private HashMap<String, Piece> pieces;

            public void removePlayer(Piece piece) {
                removePlayer(piece.name);
                piece.col = -1;
                piece.row = -1;
            }

            public void removePlayer(String name) {
                pieces.remove(name);
            }

            public void addPlayer(Piece piece) {
                piece.col = this.col;
                piece.row = this.row;
                pieces.put(piece.name, piece);
            }

            public Room(boolean red, boolean green, boolean yellow, int row, int col) {
                pieces = new HashMap<String, Piece>();
                this.row = row;
                this.col = col;
                gems[Suspicion.RED] = red;
                gems[Suspicion.GREEN] = green;
                gems[Suspicion.YELLOW] = yellow;
                String temp = "";
                if (red) temp += "red,";
                if (green) temp += "green,";
                if (yellow) temp += "yellow,";
                availableGems = (temp.substring(0, temp.length() - 1)).split(",");
            }
        }

        public void movePlayer(Piece player, int row, int col) {
            rooms[player.row][player.col].removePlayer(player);
            rooms[row][col].addPlayer(player);
        }

        public void clearRooms() {
            rooms = new Room[3][4];
            rooms[0][0] = new Room(true, false, true, 0, 0);
            rooms[0][1] = new Room(true, false, true, 0, 1);
            rooms[0][2] = new Room(true, false, true, 0, 2);
            rooms[0][3] = new Room(true, false, true, 0, 3);
            rooms[1][0] = new Room(true, false, true, 1, 0);
            rooms[1][1] = new Room(true, false, true, 1, 1);
            rooms[1][2] = new Room(true, false, true, 1, 2);
            rooms[1][3] = new Room(true, false, true, 1, 3);
            rooms[2][0] = new Room(true, false, true, 2, 0);
            rooms[2][1] = new Room(true, false, true, 2, 1);
            rooms[2][2] = new Room(true, false, true, 2, 2);
            rooms[2][3] = new Room(true, false, true, 2, 3);

        }

        public Board(String piecePositions, HashMap<String, Piece> pieces) {
            Piece piece;
            clearRooms();
            int col = 0;
            int row = 0;
            for (String room : piecePositions.split(":", -1)) // Split out each room
            {
                room = room.trim();
                if (room.length() != 0) for (String guest : room.split(",")) // Split guests out of each room
                    {
                        guest = guest.trim();
                        piece = pieces.get(guest);
                        rooms[row][col].addPlayer(piece);
                    }
                col++;
                row = row + col / 4;
                col = col % 4;
            }
        }
    }

    public Piece getPiece(String name) {
        return pieces.get(name);
    }

    public class Player {

        public String playerName;
        public ArrayList<String> possibleGuestNames;

        public void adjustKnowledge(ArrayList<String> possibleGuests) {
            Iterator<String> it = possibleGuestNames.iterator();
            while (it.hasNext()) {
                String g;
                if (!possibleGuests.contains(g = it.next())) {
                    it.remove();
                }
            }
        }

        public void removeGuessFromPossibles(String guess) {
            possibleGuestNames.remove(guess);
        }

        public Player(String name, String[] guests) {
            playerName = name;
            possibleGuestNames = new ArrayList<String>();
            for (String g : guests) {
                possibleGuestNames.add(g);
            }
        }
    }

    public class Piece {

        public int row, col;
        public String name;

        public Piece(String name) {
            this.name = name;
        }
    }

    private String[] getPossibleMoves(Piece p) {
        LinkedList<String> moves = new LinkedList<String>();
        if (p.row > 0) moves.push((p.row - 1) + "," + p.col);
        if (p.row < 2) moves.push((p.row + 1) + "," + p.col);
        if (p.col > 0) moves.push((p.row) + "," + (p.col - 1));
        if (p.col < 3) moves.push((p.row) + "," + (p.col + 1));

        return moves.toArray(new String[moves.size()]);
    }

    public String getPlayerActions(String d1, String d2, String card1, String card2, String board) throws Suspicion.BadActionException {
        this.board = new Board(board, pieces);
        String actions = "";

        // Random move for dice1
        if (d1.equals("?"))
            d1 = allCharacters[rand.nextInt(allCharacters.length)];
        Piece piece = pieces.get(d1);
        String[] moves = getPossibleMoves(piece);
        int movei = rand.nextInt(moves.length);
        actions += "move," + d1 + "," + moves[movei];
        this.board.movePlayer(piece, Integer.parseInt(moves[movei].split(",")[0]), Integer.parseInt(moves[movei].split(",")[1])); // Perform the move on my board

        // Random move for dice2
        if (d2.equals("?"))
            d2 = allCharacters[rand.nextInt(allCharacters.length)];
        piece = pieces.get(d2);
        moves = getPossibleMoves(piece);
        movei = rand.nextInt(moves.length);
        actions += ":move," + d2 + "," + moves[movei];
        this.board.movePlayer(piece, Integer.parseInt(moves[movei].split(",")[0]), Integer.parseInt(moves[movei].split(",")[1])); // Perform the move on my board

        // which card
        int i = rand.nextInt(2);
        actions += ":play,card" + (i + 1);

        String card = i == 0 ? card1 : card2;

        for (String cardAction : card.split(":")) // just go ahead and do them in this order
        {
            if (cardAction.startsWith("move")) {
                String guest;
                guest = allCharacters[rand.nextInt(allCharacters.length)];
                piece = pieces.get(guest);
                //moves = getPossibleMoves(piece);
                actions += ":move," + guest + "," + rand.nextInt(3) + "," + rand.nextInt(4);
            } else if (cardAction.startsWith("viewDeck")) {
                actions += ":viewDeck";
            } else if (cardAction.startsWith("get")) {
                if (cardAction.equals("get,"))
                    actions += ":get," + this.board.rooms[me.row][me.col].availableGems[rand.nextInt(this.board.rooms[me.row][me.col].availableGems.length)];
                else actions += ":" + cardAction;
            } else if (cardAction.startsWith("ask")) {
                String theAsk = performAsk(cardAction);
                actions += ":" + theAsk;
            }
        }
        return actions;
    }

    private String performAsk(String cardAction) {
        String guestToAskAbout = cardAction.split(",")[1];
        String returnAsk = "ask," + guestToAskAbout + "," + otherPlayerNames[rand.nextInt(otherPlayerNames.length)];
        debugPrint(returnAsk);
        return returnAsk;
    }

    public void reportPlayerActions(String player, String d1, String d2, String cardPlayed, String board, String actions) {
    }

    private boolean canSee(Piece p1, Piece p2) // returns whether or not these two pieces see each 
    {
        return (p1.row == p2.row || p1.col == p2.col);
    }

    public void answerAsk(String guest, String player, String board, boolean canSee) {
        Board b = new Board(board, pieces);
        ArrayList<String> possibleGuests = new ArrayList<String>();
        Piece p1 = pieces.get(guest);  // retrieve the guest 
        for (String k : pieces.keySet()) {
            Piece p2 = pieces.get(k);
            if ((canSee && canSee(p1, p2)) || (!canSee && !canSee(p1, p2)))
                possibleGuests.add(p2.name);
        }
        //System.out.println("Adjusting knowledge about " + player + " to : " + possibleGuests);
        players.get(player).adjustKnowledge(possibleGuests);
    }

    public void answerViewDeck(String guestName) {
        for (Player plyr : players.values())
            plyr.removeGuessFromPossibles(guestName);
    }

    public String reportGuesses() {
        String rval = "";
        if (PRINT_OUT_ALL_POSSIBLE_GUESSES) {
            for (String k : players.keySet()) {
                Player p = players.get(k);
                rval += k;
                for (String g : p.possibleGuestNames) {
                    rval += "," + g;
                }
                rval += ":";
            }
        } else {
            Map<String, String> plyrToGuessMap = getGuessBasedOnProbabilities();
            rval = "";
            for (String plyrName : players.keySet()) {
                rval += plyrName;
                rval += "," + plyrToGuessMap.get(plyrName);
                rval += ":";
            }
        }

        return rval.substring(0, rval.length() - 1);
    }

    private String getGuessBasedOnCountOfAppearances(String playerName) {
        String guess = "";
        Player player = players.get(playerName);
        Map<String, Integer> guestNameToCountMap = buildGuestNameCountMap();
        Integer lastCount = Integer.MAX_VALUE;
        for (String suspect : player.possibleGuestNames) {
            Integer count = guestNameToCountMap.get(suspect);
            if (count < lastCount) {
                guess = suspect;
                lastCount = count;
            }
        }
        return guess;
    }

    private Collection<List<String>> getAllGuessPermutations() {
        Builder<Collection<String>> inputBuilder = Stream.<Collection<String>>builder();
        for (String plyrName : players.keySet()) {
            Player player = players.get(plyrName);
            inputBuilder.add(player.possibleGuestNames);
        }

        Stream<Collection<List<String>>> listified = inputBuilder.build().filter(Objects::nonNull)
                .filter(input -> !input.isEmpty())
                .map(l -> l.stream()
                .map(o -> new ArrayList<>(Arrays.asList(o)))
                .collect(Collectors.toList()));

        Collection<List<String>> combinations = listified.reduce((input1, input2) -> {
            Collection<List<String>> merged = new ArrayList<>();
            input1.forEach(permutation1 -> input2.forEach(permutation2 -> {
                List<String> combination = new ArrayList<>();
                combination.addAll(permutation1);
                combination.addAll(permutation2);
                merged.add(combination);
            }));
            return merged;
        }).orElse(new HashSet<>());

//        combinations.forEach(System.out::println);
        return combinations;
    }

    private Collection<List<String>> removeInvalidPermutations(Collection<List<String>> allPerms) {
        Collection<List<String>> filteredPermutations = new ArrayList<>();
        for (List<String> perm : allPerms) {
            Set<String> temp = new HashSet<>();
            for (String guess : perm) {
                if (!temp.contains(guess))
                    temp.add(guess);
            }
            if (perm.size() == temp.size())
                filteredPermutations.add(perm);
        }
        return filteredPermutations;
    }

    private Map<String, Integer> buildGuestNameCountMap() {
        Map<String, Integer> guestNameToCountMap = new HashMap<>();
        for (String k : players.keySet()) {
            Player p = players.get(k);
            for (String g : p.possibleGuestNames) {
                Integer gCount = guestNameToCountMap.get(g);
                if (gCount != null) {
                    gCount++;
                } else {
                    gCount = 1;
                }
                guestNameToCountMap.put(g, gCount);
            }
        }
        return guestNameToCountMap;
    }

    public Abe_RBot_03(String playerName, String guestName, int numStartingGems, String gemLocations, String[] playerNames, String[] guestNames) {
        super(playerName, guestName, numStartingGems, gemLocations, playerNames, guestNames);
        pieces = new HashMap<String, Piece>();
        ArrayList<String> possibleGuests = new ArrayList<String>();
        for (String name : guestNames) {
            pieces.put(name, new Piece(name));
            if (!name.equals(guestName)) possibleGuests.add(name);
        }
        me = pieces.get(guestName);

        players = new HashMap<String, Player>();
        for (String str : playerNames) {
            if (!str.equals(playerName))
                players.put(str, new Player(str, possibleGuests.toArray(new String[possibleGuests.size()])));
        }

        otherPlayerNames = players.keySet().toArray(new String[players.size()]);
    }

    private void debugPrintOutPlayerPossibleGuesses() {
        for (String k : players.keySet()) {
            Player p = players.get(k);
            System.out.println(k + " -> " + p.possibleGuestNames);
        }
    }

    private void debugPrint(String str) {
        System.out.println("");
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        System.out.println(str);
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    }
}
