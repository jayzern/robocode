package u1500212.additional;

import java.awt.geom.*;

public class MeleeBullet
{
    Point2D startPoint, lastPoint;
    double startgunheading, direction, bulletspeed, bulletd;
    long lasttime;
    int[] segment;
        
    public boolean updateEnemy(Point2D enemy, long time)
    {
        long dtime;
        double dx = (enemy.getX()-lastPoint.getX())/(dtime = time-lasttime);
        double dy = (enemy.getY()-lastPoint.getY())/dtime;
        while (lasttime < time)
        {
            if (startPoint.distance(lastPoint) <= bulletd)  
            {
                segment[Math.min(30, Math.max(0, (int)Math.round((1+robocode.util.Utils.normalRelativeAngle(JekyllHyde.angle(lastPoint, startPoint)-startgunheading)/direction)*15)))]++;
                return true;
            }
            lasttime++;
            bulletd += bulletspeed;
            lastPoint.setLocation(lastPoint.getX()+dx, lastPoint.getY()+dy);
        }
        return false;
    }
}