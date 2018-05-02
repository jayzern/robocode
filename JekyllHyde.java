package u1500212;

import u1500212.additional.*;
import robocode.*;
import robocode.Robot;
import java.util.*;
import java.awt.geom.*;
import java.awt.*;

public class JekyllHyde extends Robot
{
    // Create Movement and Gun Mechanisms
    protected GuessFactorTargeting gun = null;
    protected WallCrawler wall = null;
    protected MinimumRiskMovement minimumRisk = null;
    protected WaveSurfer wave = null;

    // Shared between Minimum Risk Movement and GuessFactor
    static Point2D myLocation, last;
    static EnemyInfo currentTarget;
    static HashMap<String, EnemyInfo> enemies;
    static HashMap<String, int[][][][]> stats = new HashMap<String, int[][][][]>();
    static Point2D next = currentTarget = null;
    static double myEnergy;

    // Random Number between 1 and 10
    // Used to alternate between Minimum Risk Movement and Wave Surfing
    // During 1v1 mode,
    //      when random Number is 1, adopt Wave Surfing to move location to add randomness
    //      when random Number is 2-10, adopt Minimum Risk Movement to dodge bullets
    protected int randomNumber = 1;
    protected boolean meleeMode = true;

    public void run()
    {
        //Set bot colour
        setColour();

        gun = new GuessFactorTargeting(this);
        wall = new WallCrawler(this);
        minimumRisk = new MinimumRiskMovement(this);
        wave = new WaveSurfer(this);

        // Share enemies for Guess Factor Targeting and Minimum Risk Movement
        enemies = new HashMap<String, EnemyInfo>();

        // Check Melee or 1v1
        // Do not switch to 1v1 when there is 1 last enemy in a melee battle
        // This is because segmentations in Min Risk Movement and Guess Factor does not work well,
        // unless you are in a 1v1 condition from the beginning.
        if(getOthers() == 1)
            meleeMode = false;

        // Initialise position for wall crawling
        if(meleeMode) 
            wall.initialise();

        do {
            // Melee Strategy
            if(meleeMode) {

                // Wall Crawling
                wall.loop(); 

            // 1v1 Strategy 
            } else {
                
                // Generate Random integer between 1 to 10
                randomNumber = (int) Math.floor(Math.random() * 10) + 1;
                myLocation = new Point2D.Double(getX(), getY());

                // Alternate between Min Risk Movement and Wave Surfing
                if(randomNumber != 1) {
                    // Minimum Risk Movement
                    myEnergy = getEnergy();
                    do {
                        doTarget(); 
                    } while (currentTarget == null);
                    minimumRisk.move();
                } else {
                    // Wave Surfing
                    doTarget();
                }

            }
            
        } while (true);
    }
    
    public void onScannedRobot(ScannedRobotEvent e)
    {
        // Execute Guess Factor Gun always
        // Get others to prevent Out of bounds exception when you win
        if(getOthers() != 0)
            gun.onScannedRobot(e);

        // 1v1 Strategy
        // Execute when Wave Surfing
        if(!meleeMode) {
            if(randomNumber == 1) {
                wave.onScannedRobot(e);
            }
        }

    }

    
    public void onHitByBullet(HitByBulletEvent e)
    {        
        // 1v1 Strategy
        if(!meleeMode) {
            if(randomNumber != 1) {
                // Execute during Minimum Risk Movement
                minimumRisk.onHitByBullet(e);   
            } else {
                // Execute when Wave Surfing
                wave.onHitByBullet(e);
            }         
        }

    }

    public void onHitRobot(HitRobotEvent e) 
    {   
        // Melee Strategy 
        // Execute when wall crawling
        if(meleeMode)
            wall.onHitRobot(e);
    }

    public void onRobotDeath(RobotDeathEvent e)
    {
        // Execute for Guess Factor Targeting
        gun.onRobotDeath(e);
    }

    public void onWin(WinEvent e) {
        // Celebrate
        int i = 1;
        while (i < 360) {
            turnRight(i);
            turnLeft(i);
            i++;
        }
        
    }

    private void doTarget() {

        // Search for a target if no target is available
        if (currentTarget == null) {
            turnRadarRight(360);

        // Track the enemy using previous enemy Location
        } else {
            double an = robocode.util.Utils.normalRelativeAngleDegrees(Math.toDegrees(angle(myLocation, currentTarget)) - getRadarHeading());
            currentTarget = null;           
            turnRadarRight(an);

            // If enemy is lost, find again using remaining angles
            if (currentTarget == null) {
                if (an < 0)
                    turnRadarRight(-360 - an);
                else
                    turnRadarRight(360 - an);
            }
        }
    }

    /*******************
    * UTILITY FUNCTIONS
    ********************/

    // Calculates the location of a target point based on a source point, angle and distance.
    // Elementary math formulas
    public static Point2D.Double projectPoint(Point2D startPoint, double theta, double dist)
    {
        return new Point2D.Double(startPoint.getX() + dist * Math.sin(theta), startPoint.getY() + dist * Math.cos(theta));
    }

    // Calculates the absolute bearing between 2 points
    public static double angle(Point2D point2, Point2D point1)
    {
        return Math.atan2(point2.getX()-point1.getX(), point2.getY()-point1.getY());
    }
    
    // Convert Heading to Radians
    public double getHeadingRadians() {
        return Math.toRadians(getHeading());
    }

    // Convert Gun Heading to Radians
    public double getGunHeadingRadians() {
        return Math.toRadians(getGunHeading());
    }

    // Convert Radar Heading to Radians
    public double getRadarHeadingRadians() {
        return Math.toRadians(getRadarHeading());
    }

    // Tests whether a point is contained in a field. Used for avoiding walls
    public boolean inField(Point2D p)
    {
        return new Rectangle2D.Double(30, 30, getBattleFieldWidth()-60, getBattleFieldHeight()-60).contains(p);
    }

    // Distinguish Robot
    public void setColour() {
        setBodyColor(Color.black);
        setGunColor(Color.black);
        setRadarColor(Color.black);
        setBulletColor(Color.cyan);
        setScanColor(Color.black);
    }
}