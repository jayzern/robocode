package u1500212.additional;

import u1500212.JekyllHyde;
import robocode.*;

public class WallCrawler {

    JekyllHyde r;
    double moveAmount;

    public WallCrawler(JekyllHyde robot) {

        r = robot;

    }

    public void initialise() {
        // Set max movement amount
        moveAmount = Math.max(r.getBattleFieldWidth(), r.getBattleFieldHeight());
        
        // Make robot perpendicular
        r.turnLeft(r.getHeading() % 90);

        // Find a wall and start crawling
        r.ahead(moveAmount);
        r.turnGunRight(90);
        r.turnRight(90);
    }

    public void loop() {
        // TODO: Change guess factor to come later. Find a safe position first
        System.out.println(moveAmount);
        // Search for targeting before crawling
        r.turnRadarRight(360);

        // Crawl
        r.ahead(moveAmount);
        r.turnRight(90);
    }

    public void onHitRobot(HitRobotEvent e) {
        
        // Set back if enemy in front
        if (e.getBearing() > -90 && e.getBearing() < 90) {
            r.back(100);
        
        // Set ahead if enemy behind
        } else {
            r.ahead(100);
        } 
    }
}