package u1500212.additional;

import u1500212.JekyllHyde;
import robocode.*;
import java.util.*;
import java.awt.geom.*;

public class GuessFactorTargeting {

    JekyllHyde r;

    public GuessFactorTargeting(JekyllHyde robot) {

        r = robot;

    }

    public void onScannedRobot(ScannedRobotEvent e) {
        
        // Retrieve or create enemy and store it
        String name;
        EnemyInfo enemy = (EnemyInfo)JekyllHyde.enemies.get(name = e.getName());
        if (enemy == null)
            JekyllHyde.enemies.put(name, enemy = new EnemyInfo());

        // Segmentations are as follows: 
        //
        //      [melee / 1v1]
        //      [Lateral direction(melee) / Acceleration(1v1)]
        //      [Bullet Flight Time]
        //      [Guess Factors]
        //
        // Used to learn behaviour of enemies to improve accuracy
        int[][][][] currentStats = (int[][][][])JekyllHyde.stats.get(name);
        if (currentStats == null)
        {
            JekyllHyde.stats.put(name, currentStats = new int[2][3][13][31]);
        }

        // Update Enemy Location and Energy
        enemy.energy = e.getEnergy();
        double distance, absBearing;
        Point2D loc, myLocation;
        enemy.setLocation(loc = JekyllHyde.projectPoint(myLocation = new Point2D.Double(r.getX(), r.getY()), absBearing = r.getHeadingRadians()+e.getBearingRadians(), distance = e.getDistance()));
        
        // TODO: uncomment this for melee Guess Factor Targeting
        //if (JekyllHyde.currentTarget == null || distance < JekyllHyde.myLocation.distance(JekyllHyde.currentTarget))
            JekyllHyde.currentTarget = enemy;

        // Update segmentations on each scan
        double velocity;
        int accl = (int)Math.round(Math.abs(enemy.velocity)-Math.abs(enemy.velocity = velocity = e.getVelocity()));
        if (accl != 0)
            accl = (accl < 0) ? 1 : 2;
        double latd = e.getHeadingRadians()-absBearing;
        double bulletv;
        double power;
        double energy;
        int[] current = currentStats[Math.min(r.getOthers()-1, 1)][r.getOthers() == 1 ? accl : (int)(Math.cos(latd)*velocity/Math.abs(velocity)*1.4+1.4)][(int)(distance/(bulletv = 20-3*(power = Math.min(Math.min(3, 1200/distance), Math.min(r.getEnergy(), energy = enemy.energy = e.getEnergy())/4)))/15)];
        double direction = (((Math.sin(latd)*velocity)< 0) ? -1 : 1)*Math.asin(8/bulletv);
        Vector<MeleeBullet> waves = enemy.waves;
        int i=waves.size();
        while (i > 0)
        {
            i--;
            if (((MeleeBullet)waves.elementAt(i)).updateEnemy(loc, r.getTime()))
                waves.removeElementAt(i);
        }

        // Create a wave to store segmentations and information related to guns
        MeleeBullet wave;
        waves.add(wave = new MeleeBullet());
        wave.startPoint = myLocation;
        wave.startgunheading = absBearing;
        wave.direction = direction;
        wave.lastPoint = loc;
        wave.bulletspeed = bulletv;
        wave.lasttime = r.getTime();
        wave.segment = current;
        
        // Calculate the point of the highest probability of being shot
        if (enemy == JekyllHyde.currentTarget)
        {
            int bestindex = 15;
            double shotBearing = absBearing;
            if (energy > 0)
                do
                {
                    double tempBearing;
                    if (r.inField(JekyllHyde.projectPoint(myLocation, tempBearing = absBearing + direction*(i/15.0-1), e.getDistance()*(bulletv/(bulletv+8)))) && current[i] > current[bestindex])
                    {
                        bestindex = i;
                        shotBearing = tempBearing;
                    }
                    i++;
                }
                while (i < 31);
            
            // Face enemy and Fire if there is energy
            // Fire power varies depending on energy levels to pace Robot throughout the game
            r.turnGunRight(Math.toDegrees(robocode.util.Utils.normalRelativeAngle(shotBearing-r.getGunHeadingRadians())));
            if (energy/r.getEnergy() < 5)
                r.fire(power);
        }
    }

    public void onRobotDeath(RobotDeathEvent e)
    {
        // Remove enemy from HashMap if killed
        if (JekyllHyde.enemies.remove(e.getName()) == JekyllHyde.currentTarget)
            JekyllHyde.currentTarget = null;
    }
}