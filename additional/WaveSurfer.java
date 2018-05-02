package u1500212.additional;

import u1500212.JekyllHyde;
import robocode.*;
import robocode.util.Utils;
import robocode.Robot;
import java.util.*;
import java.awt.geom.*;

public class WaveSurfer {

    JekyllHyde r;

    // Segmentation for surfing statistics and location for robot and enemy
    public static int BINS = 47;
    public static double _surfStats[] = new double[BINS];
    public Point2D.Double _myLocation;
    public Point2D.Double _enemyLocation;
 
    // Stores wave information
    public ArrayList<EnemyWave> _enemyWaves;
    public ArrayList<Integer> _surfDirections;
    public ArrayList<Double> _surfAbsBearings;
 
    // Used to calculate bullet power by finding difference of energy between current and next.
    public static double _oppEnergy = 100.0;

    // Used to test whether point is in battle field
    // There is a similar function used in the main interface, JekyllHyde
    // But this has custom parameters for better surfing.
    public static Rectangle2D.Double _fieldRect = null;
    public static double WALL_STICK = 160;


    public WaveSurfer(JekyllHyde robot) {

        r = robot;

        // Create field with different parameters for wave surfing
        _fieldRect = new java.awt.geom.Rectangle2D.Double(18, 18, r.getBattleFieldWidth()-36, r.getBattleFieldHeight()-36);

        // Initialise variables
        _enemyWaves = new ArrayList<EnemyWave>();
        _surfDirections = new ArrayList<Integer>();
        _surfAbsBearings = new ArrayList<Double>();

    }

    public void onScannedRobot(ScannedRobotEvent e) {

        // Update my location
        _myLocation = new Point2D.Double(r.getX(), r.getY());

        // Calculate lateral velocity of your robot and the bearing between its enemy
        double lateralVelocity = r.getVelocity()*Math.sin(e.getBearingRadians());
        double absBearing = e.getBearingRadians() + r.getHeadingRadians();

        // Try to detect a wave
        r.turnRadarRight(Math.toDegrees(Utils.normalRelativeAngle(absBearing - r.getRadarHeadingRadians()) * 2));
        
        // Add surf directions and absolute bearings
        _surfDirections.add(0,
            new Integer((lateralVelocity >= 0) ? 1 : -1));
        _surfAbsBearings.add(0, new Double(absBearing + Math.PI));
 
        
        // Create a wave
        // Update the firetime, velocity, distance travelled, direction and fire location.
        // Used to track waves and dodge later
        double bulletPower = _oppEnergy - e.getEnergy();
        if (bulletPower < 3.01 && bulletPower > 0.09
            && _surfDirections.size() > 2) {
            EnemyWave ew = new EnemyWave();
            ew.fireTime = r.getTime() - 1;
            ew.bulletVelocity = bulletVelocity(bulletPower);
            ew.distanceTraveled = bulletVelocity(bulletPower);
            ew.direction = ((Integer)_surfDirections.get(2)).intValue();
            ew.directAngle = ((Double)_surfAbsBearings.get(2)).doubleValue();
            ew.fireLocation = (Point2D.Double)_enemyLocation.clone(); // last tick
 
            _enemyWaves.add(ew);
        }
        
        // Update the energy of enemy
        _oppEnergy = e.getEnergy();
 
        // update after EnemyWave detection, because that needs the previous
        // enemy location as the source of the wave
        _enemyLocation = project(_myLocation, absBearing, e.getDistance());
        

        // Keep updating and surfing waves 
        updateWaves();
        doSurfing();

    }

    // Constantly update waves to keep track of them
    // This is used to dodge waves when they are close and surfable.
    public void updateWaves() {
        for (int x = 0; x < _enemyWaves.size(); x++) {
            EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
 
            ew.distanceTraveled = (r.getTime() - ew.fireTime) * ew.bulletVelocity;
            if (ew.distanceTraveled >
                _myLocation.distance(ew.fireLocation) + 50) {
                _enemyWaves.remove(x);
                x--;
            }
        }
    }
 
    // Finds the wave that is approaching you so you can prepare to dodge it
    public EnemyWave getClosestSurfableWave() {
        double closestDistance = 50000;
        EnemyWave surfWave = null;
 
        for (int x = 0; x < _enemyWaves.size(); x++) {
            EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
            double distance = _myLocation.distance(ew.fireLocation)
                - ew.distanceTraveled;
 
            if (distance > ew.bulletVelocity && distance < closestDistance) {
                surfWave = ew;
                closestDistance = distance;
            }
        }
 
        return surfWave;
    }

    // Calcuate the index of segmentation based on the point it was hit.
    public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
        double offsetAngle = (absoluteBearing(ew.fireLocation, targetLocation)
            - ew.directAngle);
        double factor = Utils.normalRelativeAngle(offsetAngle)
            / maxEscapeAngle(ew.bulletVelocity) * ew.direction;
 
        return (int)limit(0,
            (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
            BINS - 1);
    }

    // Update segmentation based on where you were hit
    public void logHit(EnemyWave ew, Point2D.Double targetLocation) {
        int index = getFactorIndex(ew, targetLocation);
 
        for (int x = 0; x < BINS; x++) {
            _surfStats[x] += 1.0 / (Math.pow(index - x, 2) + 1);
        }
    }
 
    public void onHitByBullet(HitByBulletEvent e) {

        // Detect waves
        if (!_enemyWaves.isEmpty()) {
            Point2D.Double hitBulletLocation = new Point2D.Double(
                e.getBullet().getX(), e.getBullet().getY());
            EnemyWave hitWave = null;
 
            // Find one wave that could possibly hit us.
            for (int x = 0; x < _enemyWaves.size(); x++) {
                EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
 
                if (Math.abs(ew.distanceTraveled -
                    _myLocation.distance(ew.fireLocation)) < 50
                    && Math.abs(bulletVelocity(e.getBullet().getPower()) 
                        - ew.bulletVelocity) < 0.001) {
                    hitWave = ew;
                    break;
                }
            }
            
            // Remove wave after hit
            if (hitWave != null) {
                logHit(hitWave, hitBulletLocation);
                 _enemyWaves.remove(_enemyWaves.lastIndexOf(hitWave));
            }
        }
    }
 
    // Taken from: http://robowiki.net?Apollon
    // Predicts the position of the wave based on the direction
    public Point2D.Double predictPosition(EnemyWave surfWave, int direction) {
        Point2D.Double predictedPosition = (Point2D.Double)_myLocation.clone();
        double predictedVelocity = r.getVelocity();
        double predictedHeading = r.getHeadingRadians();
        double maxTurning, moveAngle, moveDir;
 
        int counter = 0; // number of ticks in the future
        boolean intercepted = false;
 
        do {
            moveAngle =
                wallSmoothing(predictedPosition, absoluteBearing(surfWave.fireLocation,
                predictedPosition) + (direction * (Math.PI/2)), direction)
                - predictedHeading;
            moveDir = 1;
 
            if(Math.cos(moveAngle) < 0) {
                moveAngle += Math.PI;
                moveDir = -1;
            }
 
            moveAngle = Utils.normalRelativeAngle(moveAngle);
 
            maxTurning = Math.PI/720d*(40d - 3d*Math.abs(predictedVelocity));
            predictedHeading = Utils.normalRelativeAngle(predictedHeading
                + limit(-maxTurning, moveAngle, maxTurning));

            // If predicted velocity and movement direction have different signs, break down.
            // otherwise accelerate
            predictedVelocity += (predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);
            predictedVelocity = limit(-8, predictedVelocity, 8);
 
            // calculate new predicted position
            predictedPosition = project(predictedPosition, predictedHeading, predictedVelocity);
 
            counter++;
 
            if (predictedPosition.distance(surfWave.fireLocation) <
                surfWave.distanceTraveled + (counter * surfWave.bulletVelocity)
                + surfWave.bulletVelocity) {
                intercepted = true;
            }
        } while(!intercepted && counter < 500);
 
        return predictedPosition;
    }
    
    // Checks the danger level of surf waves based on segmentations
    public double checkDanger(EnemyWave surfWave, int direction) {
        int index = getFactorIndex(surfWave,
            predictPosition(surfWave, direction));
 
        return _surfStats[index];
    }
    
    // Movement: 
    // When the bullet is nearby, check the danger level of each point and calculate the safest route.
    // Surf towards the safest point
    public void doSurfing() {
        EnemyWave surfWave = getClosestSurfableWave();
 
        if (surfWave == null) { return; }
 
        double dangerLeft = checkDanger(surfWave, -1);
        double dangerRight = checkDanger(surfWave, 1);
 
        double goAngle = absoluteBearing(surfWave.fireLocation, _myLocation);
        if (dangerLeft < dangerRight) {
            goAngle = wallSmoothing(_myLocation, goAngle - (Math.PI/2), -1);
        } else {
            goAngle = wallSmoothing(_myLocation, goAngle + (Math.PI/2), 1);
        }
 
        setBackAsFront(r , goAngle, r.getHeadingRadians());
    }

    // Recalculates the angle to move when robot is near the wall
    // Projects a point, if it is outside the battlefield then increase the angle orientation until it is valid.
    public double wallSmoothing(Point2D.Double botLocation, double angle, int orientation) {
        while (!_fieldRect.contains(project(botLocation, angle, 160))) {
            angle += orientation*0.05;
        }
        return angle;
    }

    /******
    * MINI CLASS FOR WAVES
    ******/

    class EnemyWave {
        Point2D.Double fireLocation;
        long fireTime;
        double bulletVelocity, directAngle, distanceTraveled;
        int direction;
 
        public EnemyWave() { }
    }
 
    /******
    * UTILITY FUNCTIONS (wave surfing edition)
    ******/

    // Similar projectPoint function in main interface
    // This one is used for easier calculations.
    public static Point2D.Double project(Point2D.Double sourceLocation, double angle, double length) {
        return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
            sourceLocation.y + Math.cos(angle) * length);
    }
    
    // Similar angle function in main interface
    // This is used based on the tutorials I followed in my references
    public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
        return Math.atan2(target.x - source.x, target.y - source.y);
    }
 
    // Finds the limit of a value
    public static double limit(double min, double value, double max) {
        return Math.max(min, Math.min(value, max));
    }
    
    // Calculates bulletVelocity
    public static double bulletVelocity(double power) {
        return (20D - (3D*power));
    }
 
    // Finds the maximum escapeangle
    public static double maxEscapeAngle(double velocity) {
        return Math.asin(8.0/velocity);
    }
    
    // Similar mechanism used in Minimum Risk Movement
    // This one basically finds the closest way to move to a point
    // Current movement is set at a fixed rate of 200
    // There is room for improvement if wave surfing can add some sort of anti gravity value to gauge risk.
    public static void setBackAsFront(Robot robot, double goAngle, double heading) {
        double angle =
            Utils.normalRelativeAngle(goAngle - heading);
        if (Math.abs(angle) > (Math.PI/2)) {
            if (angle < 0) {
                robot.turnRight(Math.toDegrees(Math.PI + angle));
            } else {
                robot.turnLeft(Math.toDegrees(Math.PI - angle));
            }
            robot.ahead(-200);
        } else {
            if (angle < 0) {
                robot.turnLeft(Math.toDegrees(-1*angle));
           } else {
                robot.turnRight(Math.toDegrees(angle));
           }
           robot.ahead(200);
        }
    }
}