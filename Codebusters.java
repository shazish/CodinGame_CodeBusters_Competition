import java.util.*;
import java.io.*;
import java.math.*;

/**
 * Send your busters out into the fog to trap ghosts and bring them home!
 **/
class Player {
    static Random rand;
    static ArrayList<Integer> ghostBusters;
    static ArrayList<Integer> scavStatus;
    static ArrayList<Integer> opponentBusters;
    static ArrayList<Integer> ghosts;
    static int homeX;
    static int homeY;

    static final int busterMult = 3;
    static final int scavMult = 3;
    static final int oppMult = 4;
    static final int ghostMult = 4;

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int bustersPerPlayer = in.nextInt(); // the amount of busters you control
        int ghostCount = in.nextInt(); // the amount of ghosts on the map
        int myTeamId = in.nextInt(); // if this is 0, your base is on the top left of the map, if it is one, on the bottom right

        rand = new Random();

        homeX = 0;
        homeY = 0;
        if ( myTeamId == 1 ) {
            homeX = 16000;
            homeY = 9000;
        }
        boolean justInitialized = false;
        scavStatus = new ArrayList<Integer>(); // used to keep targets and scavenge level for each Buster
        for (int n = 0; n < scavMult * bustersPerPlayer; n++) { scavStatus.add(0); } // dest x,y , scavenge level


        // game loop
        while (true) {
            ghosts = new ArrayList<Integer>();
            ghostBusters = new ArrayList<Integer>();

            opponentBusters = new ArrayList<Integer>();

            int entities = in.nextInt(); // the number of busters and ghosts visible to you
            for (int i = 0, tempIndex = 0; i < entities; i++) {
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
                    ghosts.add(state); // ghost stamina
                }
                else if ( entityType == myTeamId ) {
                    ghostBusters.add(x);
                    ghostBusters.add(y); // location x,y
                    ghostBusters.add(state);

                } else {
                    opponentBusters.add(x);
                    opponentBusters.add(y);
                    opponentBusters.add(entityId);
                    opponentBusters.add(state);  // stunned?
                }

            }
            mainLoop:
            for (int i = 0; i < bustersPerPlayer; i++) {
                // provide initial destination
                if ( scavStatus.get( i * scavMult + 2 ) == 0 ) {
                    System.err.println("scav call! " + scavStatus.get( i * scavMult + 2) );
                    setNewTargetPoint(i, bustersPerPlayer);
                }
                 // CASE: At least one opponent in the vicinity of the buster
                if (opponentBusters.size() > 0) {
                    ArrayList<Integer> distPerOpponent = new ArrayList<Integer>();
                    // System.err.println( "near buster " + i +" Opp*4: " + opponentBusters.size() );
                    for ( int j = 0; j < opponentBusters.size(); j += oppMult ) {

                        System.err.println( "Opp:  " +
                            opponentBusters.get(j) + "," +
                            opponentBusters.get(j + 1) + " id: " +
                            opponentBusters.get(j + 2) + " state: " +
                            opponentBusters.get(j + 3) );

                        int dist = distance( ghostBusters.get(i * busterMult),
                                     ghostBusters.get(i * busterMult + 1),
                                     opponentBusters.get(j),
                                     opponentBusters.get(j + 1)
                                    );

                        distPerOpponent.add(dist);
                    }

                    int target = ElementWithMinDist( distPerOpponent );
                    int targetDist = distPerOpponent.get (target);

                    // only STUN if already in range, otherwise don't move towards them
                    // if opponent already stunned (status 2), no need to calculate distance
                    // if carrying a ghost, ignore opponent (maybe only if paralyzed?)
                    if ( targetDist < 1760
                         && opponentBusters.get(target * oppMult + 3) != 2
                        // && ghostBusters.get(i * busterMult + 2) != 1
                        ) {
                        System.out.println("STUN " +  opponentBusters.get(target * oppMult + 2) );

                        continue mainLoop;
                    }
                }

                // CASE: Carrying a ghost
                if ( ghostBusters.get(i * busterMult + 2) == 1 ) {
                    System.err.println(i + " is carrying a ghost");
                    if ( distance( homeX,
                            homeY,
                            ghostBusters.get(i * busterMult),
                            ghostBusters.get(i * busterMult + 1) ) < 1600 )
                        System.out.println("RELEASE");  // close enough to release captured ghost
                    else {
                        System.out.println("MOVE " + homeX + " " + homeY); // move towards home
                    }

                }

                // CASE: At least one ghost in vicinity of the buster
                else if (ghosts.size() > 0 ) {
                    ArrayList<Integer> distPerGhost = new ArrayList<Integer>();
                    ArrayList<Integer> staminaPerGhost = new ArrayList<Integer>();
                    // System.err.println( "near buster " + i + " ghosts*3: " + ghosts.size() );
                    for ( int j = 0; j < ghosts.size(); j += ghostMult ) {
                        int dist = distance( ghostBusters.get(i * busterMult),
                                     ghostBusters.get(i * busterMult + 1),
                                     ghosts.get(j),
                                     ghosts.get(j + 1)
                                    );
                        int stamina = ghosts.get(j + 3);


                        distPerGhost.add(dist);
                        staminaPerGhost.add(stamina);
                    }
                    for (int k = 0; k < distPerGhost.size(); k++)
                        System.err.println("Dist of buster " + i + " to ghost " + k + " is " + distPerGhost.get(k));
                    int targetGhost = ElementWithMinDist( staminaPerGhost ); // replacing distPerGhost with staminaPerGhost
                    int targetGhostDist = distPerGhost.get (targetGhost);

                    if ( staminaPerGhost.get(targetGhost) > 10 && scavStatus.get( i * scavMult + 2 ) < 3 ) {
                        MoveNormally(i, bustersPerPlayer); // don't waste time on ghosts with high stamina unless we're not at the early stages
                        System.err.println("don't waste time on ghosts with high stamina: " + staminaPerGhost.get(targetGhost));
                    }
                    else if ( targetGhostDist < 1760 && targetGhostDist > 900 ) {
                        System.out.println("BUST " +  ghosts.get(targetGhost * ghostMult + 2) );
                    }
                    else if ( targetGhostDist > 1760 ) {
                        System.out.println("MOVE " + ghosts.get(targetGhost * ghostMult) + " " + ghosts.get(targetGhost * ghostMult + 1) );
                    }
                    else if ( targetGhostDist < 900 ) { // get back in order to be in capture range
                        System.err.println("ghost too close, getting back!");
                        System.out.println("MOVE "
                                + ( 2 * ghostBusters.get(i * busterMult) - ghosts.get(targetGhost * ghostMult) ) + " "
                                + ( 2 * ghostBusters.get(i * busterMult) - ghosts.get(targetGhost * ghostMult + 1) ) );
                    }

                    // // remove all three elements for that ghost
                    // for (int k = 2; k > -1; k--) {
                    //     ghosts.remove( targetGhost * ghostMult + k);
                    // }
                    // System.err.println( "post removal ghosts*3: " + ghosts.size() );

                }
                else { // nothing in vicinity
                    MoveNormally(i, bustersPerPlayer);
                }

            }
        }
    }

    public static int distance(int x1, int y1, int x2, int y2) {
        return (int) Math.floor( Math.sqrt( Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2) ) );
    }

    public static void MoveNormally(int busterIndex, int bustersPerPlayer) {
        if ( Math.abs( scavStatus.get(busterIndex * scavMult ) - ghostBusters.get(busterIndex * busterMult) ) < 250 &&
             Math.abs( scavStatus.get(busterIndex * scavMult + 1) - ghostBusters.get(busterIndex * busterMult + 1) ) < 250 ) {
            setNewTargetPoint(busterIndex, bustersPerPlayer);
        }
        System.out.println("MOVE " + scavStatus.get(busterIndex * scavMult) + " " + scavStatus.get(busterIndex * scavMult + 1) );
        System.err.println("Curr pos for " + busterIndex + " is " + ghostBusters.get(busterIndex * busterMult) + ", " + ghostBusters.get(busterIndex * busterMult + 1));
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

    public static void setNewTargetPoint( int busterIndex, int bustersPerPlayer ) {

        int radius = -999;
        int degree = -999;
        switch( scavStatus.get( busterIndex * scavMult  + 2 ) ) {
            case 0:
                radius = 7000;
                degree = 90;
                break;
            case 1:
                radius = 9500;
                degree = 65;
                break;
            case 2:
                radius = 13000;
                degree = 50;
                break;
            case 3:
                radius = -1; // check edges
                break;
            default:
                degree = -1;
                break;
        }

        scavStatus.set( busterIndex * scavMult + 2, scavStatus.get( busterIndex * scavMult + 2) + 1 ); // scavenge round for the buster++
        System.err.println("Scav lvl is now " + scavStatus.get( busterIndex * scavMult + 2) );

        double degreeRad =  Math.toRadians( degree * (busterIndex + 1) / (bustersPerPlayer + 1) );

        scavStatus.set( busterIndex * scavMult,
            (int) Math.floor( Math.abs( homeX - radius * Math.cos(degreeRad) ) ) );
        scavStatus.set( busterIndex * scavMult + 1,
            (int) Math.floor( Math.abs( homeY - radius * Math.sin(degreeRad) ) ) );

        if (radius == -1) { // all buster destinations should be 16000,0 or 0,9000
            scavStatus.set( busterIndex * scavMult,  15000 * (busterIndex % 2) );
            scavStatus.set( busterIndex * scavMult + 1,  8000 * ( (busterIndex + 1) % 2) );
        }

        if (degree == -1) { // make sure the new dest touches the edge
            if ( rand.nextInt(2) < 1 ) {
                scavStatus.set( busterIndex * scavMult,  rand.nextInt(16000) );
                scavStatus.set( busterIndex * scavMult + 1,  rand.nextInt(2) * 9000 );
            }
            else {
                scavStatus.set( busterIndex * scavMult,  rand.nextInt(2) * 16000 );
                scavStatus.set( busterIndex * scavMult + 1,  rand.nextInt(9000) );
            }
        }

        System.err.println("Setting new target for " + busterIndex + ": " +
            scavStatus.get( busterIndex * scavMult ) + ", " +
            scavStatus.get( busterIndex * scavMult + 1) );

    }
}
