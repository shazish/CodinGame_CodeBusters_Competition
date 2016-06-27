import java.util.*;
import java.io.*;
import java.math.*;

/**
 * Send your busters out into the fog to trap ghosts and bring them home!
 **/
class Player {
    static Random rand;
    static ArrayList<Integer> busterTargetXY;

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int bustersPerPlayer = in.nextInt(); // the amount of busters you control
        int ghostCount = in.nextInt(); // the amount of ghosts on the map
        int myTeamId = in.nextInt(); // if this is 0, your base is on the top left of the map, if it is one, on the bottom right

        rand = new Random();

        ArrayList<Integer> ghosts = new ArrayList<Integer>();
        ArrayList<Integer> ghostBusters = new ArrayList<Integer>();
        ArrayList<Integer> opponentBusters = new ArrayList<Integer>();
        busterTargetXY = new ArrayList<Integer>();

        int homeX = 0;
        int homeY = 0;
        if ( myTeamId == 1 ) {
            homeX = 16000;
            homeY = 9000;
        }

        busterTargetXY.add( Math.abs( homeX - 16000 ) );
        busterTargetXY.add( Math.abs( homeY - 9000 ) );

        // game loop
        while (true) {
            ghosts = new ArrayList<Integer>();
            ghostBusters = new ArrayList<Integer>();
            opponentBusters = new ArrayList<Integer>();

            int entities = in.nextInt(); // the number of busters and ghosts visible to you
            for (int i = 0; i < entities; i++) {
                int entityId = in.nextInt(); // buster id or ghost id
                int x = in.nextInt();
                int y = in.nextInt(); // position of this buster / ghost
                int entityType = in.nextInt(); // the team id if it is a buster, -1 if it is a ghost.
                int state = in.nextInt(); // For busters: 0=idle, 1=carrying a ghost.
                int value = in.nextInt(); // For busters: Ghost id being carried. For ghosts: number of busters attempting to trap this ghost.



                if ( entityType == -1 ) {
                    ghosts.add(x);
                    ghosts.add(y);
                    ghosts.add(entityId);
                }
                else if ( entityType == myTeamId ) {
                    ghostBusters.add(x);
                    ghostBusters.add(y);
                    ghostBusters.add(state);
                } else {
                    opponentBusters.add(x);
                    opponentBusters.add(y);
                    opponentBusters.add(entityId);
                    opponentBusters.add(state);
                }

            }
            mainLoop:
            for (int i = 0; i < bustersPerPlayer; i++) {

                 // at least one opponent in vicinity of the buster
                if (opponentBusters.size() > 0) {
                    ArrayList<Integer> distPerOpponent = new ArrayList<Integer>();
                    System.err.println( "near buster " + i +" Opp*4: " + opponentBusters.size() );
                    for ( int j = 0; j < opponentBusters.size(); j += 4 ) {

                        int dist = distance( ghostBusters.get(i * 3),
                                     ghostBusters.get(i * 3 + 1),
                                     opponentBusters.get(j),
                                     opponentBusters.get(j + 1)
                                    );

                        distPerOpponent.add(dist);
                    }

                    int target = ElementWithMinDist( distPerOpponent );
                    int targetDist = distPerOpponent.get (target);

                    // only STUN if already in range, otherwise don't move towards them
                    // if opponent already stunned (status 2), no need to calculate distance
                    if ( targetDist < 1760 && opponentBusters.get(target * 3 + 3) != 2 ) {
                        System.out.println("STUN " +  opponentBusters.get(target * 3 + 2) );
                        continue mainLoop;
                    }
                }

                // carrying a ghost
                if ( ghostBusters.get(i * 3 + 2) == 1 ) {
                    System.err.println(i + " is carrying a ghost");
                    if ( ghostBusters.get(i * 3) == homeX && ghostBusters.get(i * 3 + 1) == homeY ) // reached home
                        System.out.println("RELEASE");
                    else {
                        System.out.println("MOVE " + homeX + " " + homeY); // move towards home
                    }

                }

                // at least one ghost in vicinity of the buster
                else if (ghosts.size() > 0 ) {
                    ArrayList<Integer> distPerGhost = new ArrayList<Integer>();
                    // System.err.println( "near buster " + i + " ghosts*3: " + ghosts.size() );
                    for ( int j = 0; j < ghosts.size(); j += 3 ) {
                        int dist = distance( ghostBusters.get(i * 3),
                                     ghostBusters.get(i * 3 + 1),
                                     ghosts.get(j),
                                     ghosts.get(j + 1)
                                    );

                        distPerGhost.add(dist);
                    }
                    for (int k = 0; k < distPerGhost.size(); k++)
                        System.err.println("Dist of buster " + i + "to ghost " + k + " is " + distPerGhost.get(k));
                    int targetGhost = ElementWithMinDist( distPerGhost );
                    int targetGhostDist = distPerGhost.get (targetGhost);


                    if ( targetGhostDist < 1760 && targetGhostDist > 900 ) {
                        System.out.println("BUST " +  ghosts.get(targetGhost * 3 + 2) );
                    }
                    else {
                        System.out.println("MOVE " + ghosts.get(targetGhost * 3) + " " + ghosts.get(targetGhost * 3 + 1) );
                    }

                    // remove all three elements for that ghost
                    for (int k = 2; k > -1; k--) {
                        ghosts.remove( targetGhost * 3 + k);
                    }
                    // System.err.println( "post removal ghosts*3: " + ghosts.size() );

                }
                else { // nothing in vicinity
                    if ( Math.abs( busterTargetXY.get(0) - ghostBusters.get(i * 3) ) < 500 &&
                         Math.abs( busterTargetXY.get(1) - ghostBusters.get(i * 3 + 1) ) < 500) {
                        setNewTargetPoint();
                        System.out.println("MOVE " + busterTargetXY.get(0) + " " + busterTargetXY.get(1));
                    }
                    else {
                        System.out.println("MOVE " + busterTargetXY.get(0) + " " + busterTargetXY.get(1));
                    }
                    System.err.println("Curr pos for " + i + " is " + ghostBusters.get(i * 3) + ", " + ghostBusters.get(i * 3 + 1));
                }


            }
        }
    }

    public static int distance(int x1, int y1, int x2, int y2) {
        return (int) Math.floor( Math.sqrt( Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2) ) );
    }

    public static int ElementWithMinDist(ArrayList<Integer> arr) {
        int temp = arr.get(0);
        int tempindex = 0;
        for ( int j = 1; j < arr.size(); j ++ ) {
            if ( arr.get(j) < temp ) {
                tempindex = j;
                temp = arr.get(j);
            }

        }
        return tempindex;
    }

    public static void setNewTargetPoint() {

        // make sure the new point touches the edge
        if ( rand.nextInt(2) < 1 ) {
            busterTargetXY.set( 0, rand.nextInt(16000) );
            busterTargetXY.set( 1, rand.nextInt(2) * 9000 );
        }
        else {
            busterTargetXY.set( 0, rand.nextInt(2) * 16000 );
            busterTargetXY.set( 1, rand.nextInt(9000) );
        }
        System.err.println("Setting new target " + busterTargetXY.get(0) + ", " + busterTargetXY.get(1));

    }
}
