package u1500212.additional;

import u1500212.JekyllHyde;
import robocode.*;
import robocode.util.Utils;
import java.util.*;
import java.awt.geom.*;

public class MinimumRiskMovement {

    JekyllHyde r;


    public MinimumRiskMovement(JekyllHyde robot) {

        r = robot;

    }

    /****
    * Point Generating Function and Movements
    ****/
    public void move() {

        // Start next, last and current Location
        if (JekyllHyde.next == null)
            JekyllHyde.next = JekyllHyde.last = JekyllHyde.myLocation;

        // Calcuate the distance between robot and enemy target
        // Modify distance depending on 1v1 or melee
        boolean changed = false;
        double angle = 0;
        double moveDist = JekyllHyde.myLocation.distance(JekyllHyde.currentTarget);
        moveDist = (r.getOthers() == 1 ? Math.random()*moveDist : Math.min(500, moveDist))*.5;
        
        // Generates approximately 60 points with uniform distance around
        // Use a risk function to calculate each individual point and allocate a score
        // Simultaniously check whether each point does not collide against the wall
        // Finally, select the best point with the lowest risk to be the next destination
        do
        {
            Point2D p;
            if (r.inField(p = JekyllHyde.projectPoint(JekyllHyde.myLocation, angle, moveDist)) && findRisk(p) < findRisk(JekyllHyde.next))
            {
                changed = true;
                JekyllHyde.next = p;
            }
            angle += .1;
            }
        while (angle < Math.PI*2);
        if (changed)
            JekyllHyde.last = JekyllHyde.myLocation;

        // Calculate the angle between the next and current location
        angle = JekyllHyde.angle(JekyllHyde.next, JekyllHyde.myLocation) - r.getHeadingRadians();
    
        // Finds the closest angle and direction using basic math formulas
        // For example, turning -45 degrees clockwise is better than 325 degrees anti-clockwise.
        double direction = 1;
        if(Math.cos(angle) < 0) {
            angle += Math.PI;
            direction = -1;
        }
        
        // Move to the next location
        r.turnRight(Math.toDegrees(angle = Utils.normalRelativeAngle(angle)));
        r.ahead(JekyllHyde.myLocation.distance(JekyllHyde.next)*direction);
    }

    /**
    * Risk Function
    **/
    private double findRisk(Point2D point)
    {
        double risk = 0;
        Collection<EnemyInfo> enemySet;
        Iterator<EnemyInfo> it = (enemySet = JekyllHyde.enemies.values()).iterator();
        do
        {
            EnemyInfo e = (EnemyInfo)it.next();

            // Anti-gravity value, away from enemy
            double thisrisk = (e.energy+50)/point.distanceSq(e);

            // Count the enemies closer to e than the proposed points
            // More is better because the enemy would be less likely to target us and more likely to target others
            int closer = 0;
            Iterator<EnemyInfo> it2 = enemySet.iterator();
            do
            {
                EnemyInfo e2 = (EnemyInfo)it2.next();
                if (e.distance(e2)*.9 > e.distance(point))
                    closer++;
            }
            while (it2.hasNext());

            // Multiply number by number of enemies robot is closest too.
            // If enemy is likely targeting robot, multiply by some factor based on perpendicular angles
            if (closer <= 1 || e.lastHit > r.getTime()-200 || e == JekyllHyde.currentTarget)
            {
                thisrisk *= 2+2*Math.abs(Math.cos(JekyllHyde.angle(JekyllHyde.myLocation, point) - JekyllHyde.angle(e, JekyllHyde.myLocation)));
            }
            risk += thisrisk;
        }
        while (it.hasNext());

        // Add Anti-Gravity force to the risk
        if (r.getOthers() > 1)
            risk += Math.random()/JekyllHyde.last.distanceSq(point);

        // Repel current location to discourage robot from becoming a sitting duck
        risk += Math.random()/5/JekyllHyde.myLocation.distanceSq(point);

        return risk;
    }

    public void onHitByBullet(HitByBulletEvent e) {

        // Update the last time when the robot was hit by an enemy
        // This improves risk calculation in the future
        EnemyInfo enemy;
        if ((enemy = (EnemyInfo)JekyllHyde.enemies.get(e.getName())) != null)
            enemy.lastHit = r.getTime();
    }

}