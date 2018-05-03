
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Abe_Final_RBot extends Bot {

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
    private static final Set<String> MY_PUBLIC_INFO = new HashSet<>();
    private static final HashMap<String, Player> PLAYERS_INFO = new HashMap<>();
    private static final List<Integer> MY_CURRENT_GEMS = new ArrayList();
    private static final String[] DICE = new String[2];
    private static final String[] CARDS = new String[2];

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

        MY_PUBLIC_INFO.add(B);
        MY_PUBLIC_INFO.add(M);
        MY_PUBLIC_INFO.add(R);
        MY_PUBLIC_INFO.add(T);
        MY_PUBLIC_INFO.add(V);
        MY_PUBLIC_INFO.add(E);
        MY_PUBLIC_INFO.add(N);
        MY_PUBLIC_INFO.add(D);
        MY_PUBLIC_INFO.add(L);
        MY_PUBLIC_INFO.add(S);
    }
    private Random rand = new Random();
    private Board _board = new Board();
    private String myGuestName;
    private String myPlayerName;

    public Abe_Final_RBot(String playerName, String guestName, int numStartingGems, String gemLocations, String[] playerNames, String[] guestNames) {
        super(playerName, guestName, numStartingGems, gemLocations, playerNames, guestNames);
        this.myPlayerName = playerName;
        this.myGuestName = guestName;

        // get all possible guests
        ArrayList<String> possibleGuests = new ArrayList<>();
        for (String name : guestNames) {
            if (!name.equals(guestName))
                possibleGuests.add(name);
        }

        // put togther PLAYERS_INFO
        for (String plyrName : playerNames) {
            if (!plyrName.equals(myPlayerName))
                PLAYERS_INFO.put(plyrName, new Player(plyrName, possibleGuests.toArray(new String[possibleGuests.size()])));
        }
    }

    @Override
    public String getPlayerActions(String d1, String d2, String card1, String card2, String boardString) throws Suspicion.BadActionException {
        // build up the remaining gameState items
        _board.addGuestsFromTimsBoardString(boardString);
        DICE[0] = d1;
        DICE[1] = d2;
        CARDS[0] = card1;
        CARDS[1] = card2;

        // build the gameState
        GameState gameState = new GameState(_board, PLAYERS_INFO, GUEST_INDEXES, DICE, CARDS, MY_PUBLIC_INFO, MY_CURRENT_GEMS);
        // build the possibleTurns
        TurnPossibilities possibleTurns = new TurnPossibilities(gameState);
        // get the best turn from the possibleTurns
        Turn bestTurn = possibleTurns.getBestTurn();
        // build up the actions string to be returned to Suspicion.java
        return buildActionString(bestTurn);
    }

    @Override
    public void reportPlayerActions(String player, String d1, String d2, String cardPlayed, String board, String actions) {
    }

    @Override
    public void answerAsk(String guest, String player, String board, boolean canSee) {

        // TODO: Add in logic to check if plyr has only one name left, and if they do, then remove the one guest from the other plyrs lists
    }

    @Override
    public void answerViewDeck(String guestName) {
        for (Player plyr : PLAYERS_INFO.values())
            plyr.removeGuessFromPossibles(guestName);

        // TODO: Add in logic to check if plyr has only one name left, and if they do, then remove the one guest from the other plyrs lists
    }

    @Override
    public String reportGuesses() {
        String rval = "";

        Map<String, String> plyrToGuessMap = getBestGuess();
        rval = "";
        for (String plyrName : PLAYERS_INFO.keySet()) {
            rval += plyrName;
            rval += "," + plyrToGuessMap.get(plyrName);
            rval += ":";
        }
        return rval.substring(0, rval.length() - 1);
    }

    private Map<String, String> getBestGuess() {
        Permutations permutations = new Permutations(PLAYERS_INFO, GUEST_INDEXES);
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

    private String buildActionString(Turn turn) {
        String actions = turn.getMove1().toString();
        actions += ":" + turn.getMove2().toString();
        actions += ":" + turn.getFirstAction().getActionString();
        actions += ":" + turn.getSecondAction().getActionString();
        return actions;
    }

    //<editor-fold defaultstate="collapsed" desc="Board">
    public class Board {

        private final Room[][] rooms = new Room[3][4];

        public Board() {
            initializeBoard();
        }

        public Board(Board board) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 4; c++) {
                    rooms[r][c] = new Room(board.rooms[r][c]);
                }
            }
        }

        private void initializeBoard() {
            // room gems order -> red / green / yellow
            rooms[0][0] = new Room(true, false, false, new Location(0, 0));
            rooms[0][1] = new Room(true, false, true, new Location(0, 1));
            rooms[0][2] = new Room(false, true, true, new Location(0, 2));
            rooms[0][3] = new Room(true, true, false, new Location(0, 3));
            rooms[1][0] = new Room(false, true, true, new Location(1, 0));
            rooms[1][1] = new Room(true, true, false, new Location(1, 1));
            rooms[1][2] = new Room(true, false, true, new Location(1, 2));
            rooms[1][3] = new Room(false, false, true, new Location(1, 3));
            rooms[2][0] = new Room(true, false, true, new Location(2, 0));
            rooms[2][1] = new Room(false, true, false, new Location(2, 1));
            rooms[2][2] = new Room(true, true, false, new Location(2, 2));
            rooms[2][3] = new Room(false, true, true, new Location(2, 3));
        }

        public void addGuestToRoom(String guestName, Location location) {
            rooms[location.row][location.col].putGuest(guestName);
        }

        public void moveGuestToRoom(String guestName, Location location) {
            Location locationBeforeMove = getLocation(guestName);
            rooms[locationBeforeMove.row][locationBeforeMove.col].removeGuest(guestName);
            rooms[location.row][location.col].putGuest(guestName);
        }

        public Location getLocation(String guest) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 4; c++) {
                    if (rooms[r][c].hasGuest(guest))
                        return new Location(r, c);
                }
            }
            return null;
        }

        public Set<String> getGuestsThatCantTakeGem(Integer gemToTake) {
            Set<String> rtnList = new HashSet<>();
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 4; c++) {
                    if (!rooms[r][c].getGems()[gemToTake])
                        rtnList.addAll(rooms[r][c].getGuests());
                }
            }
            return rtnList;
        }

        public boolean canSeeEachOther(String guest1, String guest2) {
            return getLocation(guest1).getRow() == getLocation(guest2).getRow()
                    || getLocation(guest1).getCol() == getLocation(guest2).getCol();
        }

        private void addGuestsFromTimsBoardString(String boardString) {
            int col = 0;
            int row = 0;
            for (String room : boardString.split(":", -1)) // Split out each room
            {
                room = room.trim();
                if (room.length() != 0) {
                    for (String guest : room.split(",")) // Split guests out of each room
                    {
                        guest = guest.trim();
                        addGuestToRoom(guestName, new Location(row, col));
                    }
                }
                col++;
                row = row + col / 4;
                col = col % 4;
            }
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Room">
    public class Room {

        private final boolean gems[] = new boolean[3];
        private final Location location;
        private final Set<String> guests = new HashSet<>();

        public Room(boolean red, boolean green, boolean yellow, Location location) {
            this.location = location;
            gems[Suspicion.RED] = red;
            gems[Suspicion.GREEN] = green;
            gems[Suspicion.YELLOW] = yellow;
        }

        public Room(Room room) {
            for (int i = 0; i < 3; i++)
                this.gems[i] = room.gems[i];
            this.location = new Location(room.getLocation());
            for (String guest : room.guests)
                this.guests.add(guest);
        }

        public Location getLocation() {
            return location;
        }

        public void removeGuest(String guestName) {
            guests.remove(guestName);
        }

        public void putGuest(String guestName) {
            guests.add(guestName);
        }

        boolean hasGuest(String guest) {
            return guests.contains(guest);
        }

        public boolean[] getGems() {
            return gems;
        }

        public Set<String> getGuests() {
            return guests;
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Player">
    public class Player {

        private String playerName;
        private ArrayList<String> possibleGuestNames = new ArrayList<String>();

        public Player(String name, String[] possibleGuests) {
            playerName = name;
            for (String g : possibleGuests) {
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

    //<editor-fold defaultstate="collapsed" desc="Turn">
    public class Turn {

        private final GameState gameStateCopy;
        private final Move move1;
        private final Move move2;
        private final Action action1;
        private final Action action2;
        private double resultingEV = 0.0;

        public Turn(GameState beforeTurnGameState, Move move1, Move move2, Action action1, Action action2) {
            this.gameStateCopy = new GameState(beforeTurnGameState);
            this.move1 = move1;
            this.move2 = move2;
            this.action1 = action1;
            this.action2 = action2;
        }

        public void performTurn() {
            gameStateCopy.getBoard().moveGuestToRoom(move1.getGuestNameToMove(), move1.getGuestLocationToMove());
            gameStateCopy.getBoard().moveGuestToRoom(move2.getGuestNameToMove(), move2.getGuestLocationToMove());

            doAction(action1);
            doAction(action2);
        }

        public Move getMove1() {
            return move1;
        }

        public Move getMove2() {
            return move2;
        }

        public Action getFirstAction() {
            return action1;
        }

        public Action getSecondAction() {
            return action2;
        }

        public Double getEV() {
            return resultingEV;
        }

        private void doAction(Action action) {
            if (action instanceof GetAction) {
                String gemToTake = ((GetAction) action).getColor();
                gameStateCopy.getMyGems().add(getGemIndex(gemToTake));
                gameStateCopy.adjustMyInfo(getGemIndex(gemToTake));
                resultingEV += gameStateCopy.calculateCurrentMyInfoEV();
                resultingEV += gameStateCopy.calculateMyGemsEV();
            } else if (action instanceof AskAction) {
                String plyrToAsk = ((AskAction) action).getPlyrNameToAsk();
                String guestToAskAbout = ((AskAction) action).getGuestToAskAbout();

                // We going to need to split the gameState into two
                // One for the 'yes' path and one for the 'no' path
                GameState gameStateForYes = new GameState(gameStateCopy);
                double yesWeight = gameStateForYes.calculateWeightForAsk("Yes", plyrToAsk, guestToAskAbout);
                boolean succesful = gameStateForYes.adjustPlayersInfo("Yes", plyrToAsk, guestToAskAbout);
                if (succesful)
                    resultingEV += yesWeight * gameStateForYes.getBestGuessEV();

                GameState gameStateForNo = new GameState(gameStateCopy);
                double noWeight = gameStateForNo.calculateWeightForAsk("No", plyrToAsk, guestToAskAbout);
                succesful = gameStateForNo.adjustPlayersInfo("No", plyrToAsk, guestToAskAbout);
                if (succesful)
                    resultingEV += noWeight * gameStateForNo.getBestGuessEV();
            } else if (action instanceof TrapdoorMoveAction) {
                String guestNameToMove = ((TrapdoorMoveAction) action).getGuestNameToMove();
                Location guestLocationToMove = ((TrapdoorMoveAction) action).getGuestLocationToMove();

            } else if (action instanceof ViewDeckAction) {

            }
        }

        private Integer getGemIndex(String gem) {
            if (gem.equals("red"))
                return 0;
            else if (gem.equals("green"))
                return 1;
            else if (gem.equals("yellow"))
                return 2;
            return -1;
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Move">
    public class Move {

        private String guestNameToMove;
        private Location guestLocationToMove;

        public Move(String guestNameToMove, Location guestLocationToMove) {
            this.guestNameToMove = guestNameToMove;
            this.guestLocationToMove = guestLocationToMove;
        }

        public String getGuestNameToMove() {
            return guestNameToMove;
        }

        public Location getGuestLocationToMove() {
            return guestLocationToMove;
        }

        @Override
        public String toString() {
            return "move," + guestNameToMove + "," + guestLocationToMove.getRow() + "," + guestLocationToMove.getCol();
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="GameState">
    public class GameState {

        private static final double CORRECT_GUESS_SCORE = 7.0;
        private Board board;
        private final Map<String, Integer> guestIndexes;
        private final String[] dice;
        private final String[] cards;
        private final HashMap<String, Player> playersInfoMap;
        private final Set<String> myPublicInfo;
        private final List<Integer> myGems;
        private Random rand = new Random();

        public GameState(Board board, HashMap<String, Player> playersInfoMap,
                Map<String, Integer> guestIndexes, String[] dice, String[] cards,
                Set<String> myPublicInfo, List<Integer> myGems) {
            this.board = board;
            this.playersInfoMap = playersInfoMap;
            this.guestIndexes = guestIndexes;
            this.dice = dice;
            this.cards = cards;
            this.myPublicInfo = myPublicInfo;
            this.myGems = myGems;
        }

        public GameState(GameState gameState) {
            this.board = new Board(gameState.getBoard());
            this.dice = gameState.getDice();
            this.cards = gameState.getCards();
            this.playersInfoMap = new HashMap<>();
            gameState.getPlayersInfoMap().entrySet().forEach((entry) -> {
                Player plyr = (Player) entry.getValue();
                int i = 0;
                String[] plyrPossGuestNamesCopy = new String[plyr.getPossibleGuestNames().size()];
                for (String str : plyr.getPossibleGuestNames()) {
                    plyrPossGuestNamesCopy[i] = str;
                    i++;
                }
                playersInfoMap.put((String) entry.getKey(), new Player(plyr.getPlayerName(), plyrPossGuestNamesCopy));
            });
            this.myPublicInfo = new HashSet<>();
            gameState.getMyPublicInfo().forEach((info) -> {
                this.myPublicInfo.add(info);
            });
            this.myGems = new ArrayList<>();
            myGems.forEach((i) -> {
                this.myGems.add(i);
            });
            this.guestIndexes = gameState.getGuestIndexes();
        }

        public void setBoard(Board board) {
            this.board = board;
        }

        public Board getBoard() {
            return board;
        }

        public Map<String, Integer> getGuestIndexes() {
            return guestIndexes;
        }

        public String[] getDice() {
            return dice;
        }

        public String[] getCards() {
            return cards;
        }

        public Action getFirstAction() {
            return getAction(cards[0]);
        }

        public Action getSecondAction() {
            return getAction(cards[1]);
        }

        public HashMap<String, Player> getPlayersInfoMap() {
            return playersInfoMap;
        }

        public Set<String> getMyPublicInfo() {
            return myPublicInfo;
        }

        public List<Integer> getMyGems() {
            return myGems;
        }

        public HashMap<String, Player> copyPlayerInfoMap() {
            HashMap<String, Player> playersInfoMapCopy = new HashMap<>();
            getPlayersInfoMap().entrySet().forEach((entry) -> {
                Player plyr = (Player) entry.getValue();
                int i = 0;
                String[] plyrPossGuestNamesCopy = new String[plyr.getPossibleGuestNames().size()];
                for (String str : plyr.getPossibleGuestNames()) {
                    plyrPossGuestNamesCopy[i] = str;
                    i++;
                }
                playersInfoMapCopy.put((String) entry.getKey(), new Player(plyr.getPlayerName(), plyrPossGuestNamesCopy));
            });
            return playersInfoMapCopy;
        }

        public void adjustMyInfo(Integer gemToTake) {
            // build the list of guests that DON'T have the gemToTake color in their room
            Set<String> guestsThatCantTakeGems = board.getGuestsThatCantTakeGem(gemToTake);
            // remove guests from myPublicInfo that are in that list
            for (String removingGuest : guestsThatCantTakeGems) {
                if (myPublicInfo.contains(removingGuest))
                    myPublicInfo.remove(removingGuest);
            }
        }

        public Double calculateCurrentMyInfoEV() {
            double numOfGuests = 10d;
            return (-1) * (numOfGuests - myPublicInfo.size()) / numOfGuests * CORRECT_GUESS_SCORE;
        }

        public Double calculateMyGemsEV() {
            return 8.0; // all of my options on the test will be this
        }

        public Map<String, String> getBestGuess() {
            Permutations permutations = new Permutations(playersInfoMap, guestIndexes);
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

        public double getBestGuessEV() {
            Permutations permutations = new Permutations(playersInfoMap, guestIndexes);
            Map<List<String>, Double> permToExpectedValueMap = permutations.getExpectedValues();

            double bestEV = Double.MIN_VALUE;
            for (Double ev : permToExpectedValueMap.values()) {
                if (ev > bestEV)
                    bestEV = ev;
            }

            return bestEV;
        }

        public double calculateWeightForAsk(String answer, String plyrToAsk, String guestToAskAbout) {
            Player plyr = playersInfoMap.get(plyrToAsk);
            double count = 0d;
            if ("yes".equalsIgnoreCase(answer)) {
                // count how many players could answer yes to seeing guessToAskAbout
                for (String s : plyr.getPossibleGuestNames()) {
                    if (board.canSeeEachOther(s, guestToAskAbout))
                        count = count + 1.0;
                }
            } else if ("no".equalsIgnoreCase(answer)) {
                for (String s : plyr.getPossibleGuestNames()) {
                    if (!board.canSeeEachOther(s, guestToAskAbout))
                        count = count + 1.0;
                }
            }
            return count / (double) plyr.getPossibleGuestNames().size();
        }

        public boolean adjustPlayersInfo(String answer, String plyrToAsk, String guestToAskAbout) {
            Player plyr = playersInfoMap.get(plyrToAsk);
            List<String> possiblesCopy = (List<String>) plyr.getPossibleGuestNames().clone();
            if ("yes".equalsIgnoreCase(answer)) {
                // need to remove any guest names from possibles that can't be seen
                for (String guest : possiblesCopy) {
                    if (!board.canSeeEachOther(guest, guestToAskAbout))
                        plyr.removeGuessFromPossibles(guest);
                }
            } else if ("no".equalsIgnoreCase(answer)) {
                for (String guest : possiblesCopy) {
                    if (board.canSeeEachOther(guest, guestToAskAbout))
                        plyr.removeGuessFromPossibles(guest);
                }
            }
            return cleanUpPlayersInfoMap();
        }

        private boolean cleanUpPlayersInfoMap() {
            HashMap<String, Player> copy = copyPlayerInfoMap();
            for (Map.Entry entry : copy.entrySet()) {
                String plyrName = (String) entry.getKey();
                Player plyr = (Player) entry.getValue();

                if (plyr.getPossibleGuestNames().isEmpty()) {
                    return false;
                }
                if (plyr.getPossibleGuestNames().size() == 1) {
                    for (Player origP : playersInfoMap.values()) {
                        if (!origP.getPlayerName().equals(plyrName)) {
                            origP.removeGuessFromPossibles(plyr.getPossibleGuestNames().get(0));
                        }
                    }
                }
            }
            return true;
        }

        private Action getAction(String card) {
            if (card.startsWith("get"))
                return new GetAction();
            else if (card.startsWith("ask"))
                return new AskAction();
            else if (card.startsWith("move"))
                return new TrapdoorMoveAction();
            else if (card.startsWith("viewDeck"))
                return new ViewDeckAction();
            return null;
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="TurnPossibilities">
    public class TurnPossibilities {

        private final Map<Turn, Double> turnsWithEVs = new HashMap<>();
        private final List<Turn> possibleTurns = new ArrayList<>();
        private final GameState gameState;

        public TurnPossibilities(GameState gameState) {
            this.gameState = gameState;
            buildPossibleTurnList();
            addEVCalculationsToPossibleTurns();
        }

        private void buildPossibleTurnList() {
            String moveGuest1 = gameState.getDice()[0];
            String moveGuest2 = gameState.getDice()[1];

            Location locationGuest1 = gameState.getBoard().getLocation(moveGuest1);
            List<Location> possibleMoveLocationsForGuest1 = locationGuest1.getPossibleNextLocations();
            Location locationGuest2 = gameState.getBoard().getLocation(moveGuest2);
            List<Location> possibleMoveLocationsForGuest2 = locationGuest2.getPossibleNextLocations();

            for (Location nextLocGuest1 : possibleMoveLocationsForGuest1) {
                for (Location nextLocGuest2 : possibleMoveLocationsForGuest2) {
                    for (int i = 0; i < gameState.getTakeGemOptions().length; i++) {
                        for (int j = 0; j < gameState.getAskAboutGuestOptions().length; j++) {
                            for (int k = 0; k < gameState.getPlayersInfoMap().size(); k++) {
                                Move move1 = new Move(moveGuest1, nextLocGuest1);
                                Move move2 = new Move(moveGuest2, nextLocGuest2);
                                String action1 = "TakeGem," + gameState.getTakeGemOptions()[i];
                                String action2 = "Ask," + gameState.getPlayersInfoMap().keySet().toArray()[k] + "," + gameState.getAskAboutGuestOptions()[j];
                                possibleTurns.add(new Turn(gameState, move1, move2, action1, action2));
                            }
                        }
                    }
                }
            }
        }

        public List<Turn> getPossibleTurns() {
            return possibleTurns;
        }

        private void addEVCalculationsToPossibleTurns() {
            for (Turn turn : possibleTurns) {
                turn.performTurn();
                turnsWithEVs.put(turn, turn.getEV());
            }
        }

        public Map<Turn, Double> getTurnsWithEVs() {
            return turnsWithEVs;
        }

        public Turn getBestTurn() {
            double bestEV = Double.MIN_VALUE;
            for (Double ev : turnsWithEVs.values()) {
                if (ev > bestEV)
                    bestEV = ev;
            }

            List<Turn> bestTurns = new ArrayList<>();
            for (Map.Entry entry : turnsWithEVs.entrySet()) {
                Turn turn = (Turn) entry.getKey();
                Double ev = (Double) entry.getValue();

                if (ev >= bestEV) {
                    bestTurns.add(turn);
                }
            }
            // if more than one bestTurns, get a random one of them
            return bestTurns.get(rand.nextInt(bestTurns.size()));
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Location">
    public class Location {

        public int row, col;

        public Location(int row, int col) {
            this.row = row;
            this.col = col;
        }

        public Location(Location location) {
            this.row = location.getRow();
            this.col = location.getCol();
        }

        public int getRow() {
            return row;
        }

        public int getCol() {
            return col;
        }

        List<Location> getPossibleNextLocations() {
            List<Location> poss = new ArrayList<>();

            if (row - 1 >= 0)
                poss.add(new Location(row - 1, col));
            if (row + 1 <= 2)
                poss.add(new Location(row + 1, col));
            if (col - 1 >= 0)
                poss.add(new Location(row, col - 1));
            if (col + 1 <= 3)
                poss.add(new Location(row, col + 1));

            return poss;
        }

        @Override
        public String toString() {
            return row + "," + col;
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Action">
    public interface Action {

        public String getActionString();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="ViewDeckAction">
    public class ViewDeckAction implements Action {

        @Override
        public String getActionString() {
            return "viewDeck";
        }

    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="GetAction">
    public class GetAction implements Action {

        private String color;

        public void setColor(String color) {
            this.color = color;
        }

        public String getColor() {
            return color;
        }

        @Override
        public String getActionString() {
            return "get," + color;
        }

    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="AskAction">
    public class AskAction implements Action {

        private String guestToAskAbout;
        private String plyrNameToAsk;

        public void setGuestToAskAbout(String guestToAskAbout) {
            this.guestToAskAbout = guestToAskAbout;
        }

        public void setPlyrNameToAsk(String plyrNameToAsk) {
            this.plyrNameToAsk = plyrNameToAsk;
        }

        public String getGuestToAskAbout() {
            return guestToAskAbout;
        }

        public String getPlyrNameToAsk() {
            return plyrNameToAsk;
        }

        @Override
        public String getActionString() {
            return "ask," + guestToAskAbout + "," + plyrNameToAsk;
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="TrapdoorMoveAction">
    public class TrapdoorMoveAction implements Action {

        private String guestNameToMove;
        private Location guestLocationToMove;

        public String getGuestNameToMove() {
            return guestNameToMove;
        }

        public Location getGuestLocationToMove() {
            return guestLocationToMove;
        }

        public void setGuestNameToMove(String guestNameToMove) {
            this.guestNameToMove = guestNameToMove;
        }

        public void setGuestLocationToMove(Location guestLocationToMove) {
            this.guestLocationToMove = guestLocationToMove;
        }

        @Override
        public String getActionString() {
            return "move," + guestNameToMove + "," + guestLocationToMove.getRow() + "," + guestLocationToMove.getCol();
        }

    }
    //</editor-fold>
}
