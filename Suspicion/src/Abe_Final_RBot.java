
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Abe_Final_RBot extends Bot {

    private static final boolean PRINT_OUT_ALL_POSSIBLE_GUESSES = false;
    private static final String B = "Buford Barnswallow";
    private static final String M = "Mildred Wellington";
    private static final String R = "Remy La Rocque";
    private static final String T = "Trudie Mudge";
    private static final String V = "Viola Chung";
    private static final String E = "Earl of Volesworthy";
    private static final String N = "Nadia Bwalya";
    private static final String D = "Dr. Ashraf Najem";
    private static final String L = "Lily Nesbit";
    private static final String S = "Stefano Laconi";
    private static final Map<String, Integer> GUEST_INDEXES = new HashMap<>();
    private static final String[] ALL_CHARACTERS = {B, M, R, T, V, E, N, D, L, S};

    static {
        GUEST_INDEXES.put(B, 0);
        GUEST_INDEXES.put(T, 1);
        GUEST_INDEXES.put(R, 2);
        GUEST_INDEXES.put(V, 3);
        GUEST_INDEXES.put(S, 4);
        GUEST_INDEXES.put(L, 5);
        GUEST_INDEXES.put(E, 6);
        GUEST_INDEXES.put(N, 7);
        GUEST_INDEXES.put(D, 8);
        GUEST_INDEXES.put(M, 9);
    }
    private Random rand = new Random();
    private HashMap<String, Piece> pieces; // <GuestName, Piece>
    private Board board;
    private Piece me;
    private HashMap<String, Player> playersInfoMap; // <PlayerName, Player>
    private String otherPlayerNames[];

    public Abe_Final_RBot(String playerName, String guestName, int numStartingGems, String gemLocations, String[] playerNames, String[] guestNames) {
        super(playerName, guestName, numStartingGems, gemLocations, playerNames, guestNames);
        pieces = new HashMap<>();
        ArrayList<String> possibleGuests = new ArrayList<>();
        for (String name : guestNames) {
            pieces.put(name, new Piece(name));
            if (!name.equals(guestName)) possibleGuests.add(name);
        }
        me = pieces.get(guestName);

        playersInfoMap = new HashMap<>();
        for (String str : playerNames) {
            if (!str.equals(playerName))
                playersInfoMap.put(str, new Player(str, possibleGuests.toArray(new String[possibleGuests.size()])));
        }

        otherPlayerNames = playersInfoMap.keySet().toArray(new String[playersInfoMap.size()]);
    }

    public Piece getPiece(String name) {
        return pieces.get(name);
    }

    private String[] getPossibleMoves(Piece p) {
        LinkedList<String> moves = new LinkedList<>();
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
            d1 = ALL_CHARACTERS[rand.nextInt(ALL_CHARACTERS.length)];
        Piece piece = pieces.get(d1);
        String[] moves = getPossibleMoves(piece);
        int movei = rand.nextInt(moves.length);
        actions += "move," + d1 + "," + moves[movei];
        this.board.movePlayer(piece, Integer.parseInt(moves[movei].split(",")[0]), Integer.parseInt(moves[movei].split(",")[1])); // Perform the move on my board

        // Random move for dice2
        if (d2.equals("?"))
            d2 = ALL_CHARACTERS[rand.nextInt(ALL_CHARACTERS.length)];
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
                guest = ALL_CHARACTERS[rand.nextInt(ALL_CHARACTERS.length)];
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
        ArrayList<String> possibleGuests = new ArrayList<>();
        Piece p1 = pieces.get(guest);  // retrieve the guest 
        for (String k : pieces.keySet()) {
            Piece p2 = pieces.get(k);
            if ((canSee && canSee(p1, p2)) || (!canSee && !canSee(p1, p2)))
                possibleGuests.add(p2.name);
        }
        //System.out.println("Adjusting knowledge about " + player + " to : " + possibleGuests);
        playersInfoMap.get(player).adjustKnowledge(possibleGuests);
    }

    public void answerViewDeck(String guestName) {
        for (Player plyr : playersInfoMap.values())
            plyr.removeGuessFromPossibles(guestName);
    }

    public String reportGuesses() {
        String rval = "";

        Map<String, String> plyrToGuessMap = getBestGuess();
        rval = "";
        for (String plyrName : playersInfoMap.keySet()) {
            rval += plyrName;
            rval += "," + plyrToGuessMap.get(plyrName);
            rval += ":";
        }
        return rval.substring(0, rval.length() - 1);
    }

    public Map<String, String> getBestGuess() {
        Permutations permutations = new Permutations(playersInfoMap, GUEST_INDEXES);
        Map<List<String>, Double> permToExpectedValueMap = permutations.getExpectedValues();

        double bestEV = Double.MIN_VALUE;
        for (Double ev : permToExpectedValueMap.values()) {
            if (ev > bestEV)
                bestEV = ev;
        }

        List<Map<String, String>> combos = new ArrayList<>();
        for (Map.Entry entry : permToExpectedValueMap.entrySet()) {
            List<String> perm = (List<String>) entry.getKey();
            Double ev = (Double) entry.getValue();

            if (ev >= bestEV) {
                Map<String, String> plyrToGuessMap = new HashMap<>();
                for (int i = 0; i < perm.size(); i++) {
                    plyrToGuessMap.put(permutations.getPlayerNames().get(i), perm.get(i));
                }
                combos.add(plyrToGuessMap);
            }
        }
        return combos.get(rand.nextInt(combos.size()));
    }

    private void debugPrint(String str) {
        System.out.println("");
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        System.out.println(str);
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    }

    //<editor-fold defaultstate="collapsed" desc="Board">
    public class Board {

        public Room rooms[][];

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
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Room">
    public class Room {

        public final boolean gems[] = new boolean[3];
        public final String[] availableGems;
        public final int row;
        public final int col;
        private HashMap<String, Piece> pieces;

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
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Player">
    public class Player {

        private String playerName;
        private ArrayList<String> possibleGuestNames = new ArrayList<String>();

        public Player(String name, String[] guests) {
            playerName = name;
            for (String g : guests) {
                possibleGuestNames.add(g);
            }
        }

        public String getPlayerName() {
            return playerName;
        }

        public ArrayList<String> getPossibleGuestNames() {
            return possibleGuestNames;
        }

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
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Piece">
    public class Piece {

        public int row, col;
        public String name;

        public Piece(String name) {
            this.name = name;
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Permutations">
    public class Permutations {

        private static final double CORRECT_GUESS_SCORE = 7.0;
        private final HashMap<String, Player> playersInfoMap;
        private final Map<String, Integer> guestIndexes;
        private Collection<List<String>> permutations;
        private final List<String> playerNames = new ArrayList<>();

        public Permutations(HashMap<String, Player> playersInfoMap, Map<String, Integer> guestIndexes) {
            this.playersInfoMap = playersInfoMap;
            this.guestIndexes = guestIndexes;
            createAllGuessPermutations();
            removeInvalidPermutations();
        }

        public Iterable<List<String>> getPermutations() {
            return permutations;
        }

        public Map<List<String>, Double> getExpectedValues() {
            Map<List<String>, Double> permToExpectedValueMap = new HashMap<>();

            int[][] permCountMatrix = buildPermCountMatrix(permutations);
            for (List<String> perms : permutations) {
                permToExpectedValueMap.put(perms, calculateEV(permutations.size(), perms, permCountMatrix));
            }
            return permToExpectedValueMap;
        }

        public List<String> getPlayerNames() {
            return playerNames;
        }

        private void createAllGuessPermutations() {
            Stream.Builder<Collection<String>> inputBuilder = Stream.<Collection<String>>builder();
            for (String plyrName : playersInfoMap.keySet()) {
                Player player = playersInfoMap.get(plyrName);
                playerNames.add(plyrName);
                inputBuilder.add(player.getPossibleGuestNames());
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

            permutations = combinations;
        }

        private void removeInvalidPermutations() {
            Collection<List<String>> filteredPermutations = new ArrayList<>();
            for (List<String> perm : permutations) {
                Set<String> temp = new HashSet<>();
                for (String guess : perm) {
                    if (!temp.contains(guess))
                        temp.add(guess);
                }
                if (perm.size() == temp.size())
                    filteredPermutations.add(perm);
            }
            permutations = filteredPermutations;
        }

        private int[][] buildPermCountMatrix(Collection<List<String>> filteredPerms) {
            int[][] permCountMatrix = new int[guestIndexes.size()][playersInfoMap.size()];

            for (List<String> perms : filteredPerms) {
                int plyrIndex = 0;
                for (String guest : perms) {
                    int guestIndex = guestIndexes.get(guest);
                    permCountMatrix[guestIndex][plyrIndex]++;
                    plyrIndex++;
                }
            }

            return permCountMatrix;
        }

        private Double calculateEV(int numPermPoss, List<String> perms, int[][] permCountMatrix) {
            double ev = 0.0;
            double evFactor = CORRECT_GUESS_SCORE / numPermPoss;
            int plyrIndex = 0;
            for (String guest : perms) {
                ev += permCountMatrix[guestIndexes.get(guest)][plyrIndex] * evFactor;
                plyrIndex++;
            }
            return ev;
        }
    }
    //</editor-fold>
}
