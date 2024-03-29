package edu.gmu.hivgame.core;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;

import static playn.core.PlayN.assets;
import static playn.core.PlayN.graphics;
import static playn.core.PlayN.log;

import playn.core.CanvasImage;
import playn.core.Canvas;
import playn.core.DebugDrawBox2D;
import playn.core.GroupLayer;
import playn.core.ImageLayer;
import playn.core.Image;
import playn.core.util.Callback;

/* Magnet object currently not appearing when game is run.
 * Possible causes: image under another image layer; no actual
 * image displaying; Physics Body has some flaw (density, etc)
 * that causes it to not appear; positioned outside of screen;
 * not updating with each time step (see paint and update methods
 * in Virus.java).
 */

public class Antibody implements CollisionHandler{
  private float prevX, prevY, prevA;
  private Body body;
  private Fixture myBodyFixture;
  private ImageLayer myLayer;
  AidsAttack game;
  private boolean attracted;

  //Mostly mimics layout of Virus class.
  private Antibody(){
  }
  public static Antibody make(AidsAttack game, float x, float y, float ang){
    Antibody a = new Antibody();
    a.game = game;
    a.initPhysicsBody(game.physicsWorld(), x, y, ang);
    //maybe need to add image layer?
    a.drawAntibodyImage();
    game.addLayer(a.myLayer);
    //currently not appearing on game screen
    a.prevX = a.x(); a.prevY = a.y(); a.prevA = a.ang();
    a.attracted = false;
    return a;
  } 

  public Body body(){ return this.body; }
  public Vec2 position(){ return this.body().getPosition(); }
  public boolean isAttracting(){return this.attracted;}

  void initPhysicsBody(World world, float x, float y, float angle){
    BodyDef bodyDef = new BodyDef();
    bodyDef.type = BodyType.DYNAMIC;
    bodyDef.position = new Vec2(x, y);
    bodyDef.angle = angle;
    bodyDef.linearDamping = 0.25f;
    Body body = world.createBody(bodyDef);

    CircleShape circleShape = new CircleShape();
    //currently same radius as Sensor added to Virus
    circleShape.m_radius = 2.0f;
    // density is 1.0f   
    this.myBodyFixture = body.createFixture(circleShape, 1.0f);
    this.myBodyFixture.m_userData = this;

    this.body = body;
    this.body.m_userData = this;
  }

  float scaleX, scaleY;

  private void drawAntibodyImage(){
    Fixture fix = body.getFixtureList();
    CircleShape s = (CircleShape) fix.getShape();
    float physRad = s.getRadius();

    float screenRad = physRad / AidsAttack.physUnitPerScreenUnit;
    //System.out.printf("Antibody physRad: %f\nscreenRad: %f\n",physRad,screenRad);
    //why random number?
    //screenRad = 50f;

    CanvasImage image = graphics().createImage(100, 100);

    Canvas canvas = image.canvas();
    //canvas.setStrokeWidth(2);
    canvas.setStrokeColor(0xff000000);
    canvas.fillCircle(50f,50f,screenRad);

    this.myLayer = graphics().createImageLayer(image);
    myLayer.setOrigin(image.width() / 2f, image.height() / 2f);
    scaleX = (getWidth()  / AidsAttack.physUnitPerScreenUnit) / image.width();
    scaleY = (getHeight() / AidsAttack.physUnitPerScreenUnit) / image.height();
    //System.out.printf("scaleX: %f\nscaleY: %f",scaleX,scaleY);
    myLayer.setScale(scaleX,scaleY);

    myLayer.setTranslation(x(), y());
    myLayer.setRotation(ang());
  }

  float getWidth(){
    return 1.0f;
  }
  float getHeight(){
    return 1.0f;
  }

  float x(){
    return body.getPosition().x;
  }
  float y(){
    return body.getPosition().y;
  }
  float ang(){
    return body.getAngle();
  }


  public void attractTowards(Vec2 target){
    Vec2 force = target.sub(position());
    float length = force.normalize();
    force.mulLocal(10f);
    body().applyForceToCenter(force);
  }


  public void paint(float alpha){
    float x = (x() * alpha) + (prevX * (1f - alpha));
    float y = (y() * alpha) + (prevY * (1f - alpha));
    float a = (ang() * alpha) + (prevA * (1f - alpha));
    myLayer.setTranslation(x, y);
    myLayer.setRotation(a);

    float angle = (game.time() + game.UPDATE_RATE*alpha) * (float) Math.PI / 1000;
  }

  public void update(float delta){
    prevX = x();
    prevY = y();
    prevA = ang();
  }

  public void handleCollision(Fixture me, Fixture other){
    if(other.m_userData instanceof Virus){
      Virus v = (Virus) other.m_userData;
      this.attractTowards(new Vec2(v.x(), v.y()));
    }
  }      

  public void destroy(){
    // remove graphics
    this.game.removeLayer(this.myLayer);
    // remove physics body
    this.game.physicsWorld().destroyBody(this.body);
  }
}
