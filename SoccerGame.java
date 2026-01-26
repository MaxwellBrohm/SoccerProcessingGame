Player player;
Ball ball;
Camera camera;
SoccerField field;
Enemy enemy;

int leftScore = 0;
int rightScore = 0;

int goalTimer = 0;

void setup() {
  fullScreen();
  field = new SoccerField(2000, 1200);
  player = new Player(0, 0);
  enemy = new Enemy(300, 0);
  ball = new Ball(0, 0);
  camera = new Camera();
}

void draw() {
  background(30, 160, 30);

  player.update();
  ball.update(player, field);
  enemy.update(ball, field);
  camera.update(player);

  pushMatrix();
  translate(-camera.x, -camera.y);

  field.draw();
  ball.draw();
  player.draw();
  enemy.draw();
  player.drawArrow(ball, camera);

  popMatrix();

  drawUI();
}

class Player {
  float x, y;
  float vx, vy;

  float accel = 0.4;
  float friction = 0.9;
  float maxSpeed = 5;

  boolean up, down, left, right;

  Player(float x, float y) {
    this.x = x;
    this.y = y;
  }

  void update() {
    float dx = 0;
    float dy = 0;

    if (right) dx++;
    if (left) dx--;
    if (up) dy--;
    if (down) dy++;

    if (dx != 0 || dy != 0) {
      float len = sqrt(dx*dx + dy*dy);
      dx /= len;
      dy /= len;
      vx += dx * accel;
      vy += dy * accel;
    } else {
      vx *= friction;
      vy *= friction;
    }

    float speed = sqrt(vx*vx + vy*vy);
    if (speed > maxSpeed) {
      vx = vx / speed * maxSpeed;
      vy = vy / speed * maxSpeed;
    }

    x += vx;
    y += vy;
  }

  void draw() {
    fill(255);
    noStroke();
    ellipse(x, y, 30, 30);
  }

    void drawArrow(Ball ball, Camera cam) {
    if (ball.owner != 1) return;
  
    float angle = atan2(
      mouseY + cam.y - y,
      mouseX + cam.x - x
    );
  
    pushMatrix();
    pushStyle();   // 🔒 save stroke/fill state
  
    translate(x, y);
    rotate(angle);
  
    stroke(150);
    strokeWeight(4);
    line(0, 0, 40, 0);
    triangle(40, 0, 25, -7, 25, 7);
  
    if (ball.charging) {
      float len = map(ball.shootPower, 0, ball.maxShootPower, 0, 40);
      stroke(0, 255, 0);
      strokeWeight(6);
      line(0, 0, len, 0);
    }
  
    popStyle();    // 🔓 restore styles
    popMatrix();
  }
}

class Ball {
  float x, y;
  float vx, vy;

  float radius = 15;
  float friction = 0.98;
  float maxSpeed = 12;

  boolean hasBall = false;
  boolean charging = false;

  float shootPower = 0;
  float maxShootPower = 15;
  int owner = 0; 

  Ball(float x, float y) {
    this.x = x;
    this.y = y;
  }

  void update(Player p, SoccerField field) {
    if (owner == 1) {
      // Player has ball
      float angle = atan2(
        mouseY + camera.y - p.y,
        mouseX + camera.x - p.x
      );
      x = p.x + cos(angle) * 30;
      y = p.y + sin(angle) * 30;
      vx = vy = 0;
    } 
    else if (owner == 2) {
      x = enemy.x + 20;
      y = enemy.y;
      vx = vy = 0;
    }

    else {
      // Free ball
      vx *= friction;
      vy *= friction;
      x += vx;
      y += vy;
    }
  
    // Player pickup
    if (owner == 0 && dist(x, y, p.x, p.y) < radius + 15) {
      owner = 1;
    }
  
    // Charging
    if (charging && owner == 1) {
      shootPower = min(shootPower + 0.25, maxShootPower);
    }
  
    // Goal check
    int goal = field.checkGoal(x, y);
    if (goal != 0) {
      if (goal == -1) rightScore++;
      if (goal == 1) leftScore++;
      goalTimer = 120;
      reset();
      return;
    }
  
    if (!field.inBounds(x, y)) {
      reset();
    }
  }


  void reset() {
    x = 0;
    y = 0;
    vx = vy = 0;
    owner = 0;
    charging = false;
    shootPower = 0;
  }


  void draw() {
    fill(0, 100, 255);
    noStroke();
    ellipse(x, y, radius*2, radius*2);
  }
}

class Camera {
  float x, y;

  void update(Player p) {
    x += (p.x - width/2 - x) * 0.1;
    y += (p.y - height/2 - y) * 0.1;
  }
}

class SoccerField {
  float w, h;
  float goalW = 200;
  float goalH = 300;

  SoccerField(float w, float h) {
    this.w = w;
    this.h = h;
  }

  void draw() {
    fill(40, 180, 40);
    rect(-w/2, -h/2, w, h);

    stroke(255);
    strokeWeight(4);
    line(0, -h/2, 0, h/2);
    noFill();
    ellipse(0, 0, 200, 200);

    // Goals
    rect(-w/2 - 20, -goalH/2, 20, goalH);
    rect(w/2, -goalH/2, 20, goalH);
  }

  boolean inBounds(float x, float y) {    
    // Top / bottom walls always solid
    if (y < -h/2 || y > h/2) return false;
  
    // Left side
    if (x < -w/2) {
      // Allow opening at goal
      if (y > -goalH/2 && y < goalH/2) return true;
      return false;
    }
  
    // Right side
    if (x > w/2) {
      if (y > -goalH/2 && y < goalH/2) return true;
      return false;
    }
  
    return true;
  }

  int checkGoal(float x, float y) {
    if (y > -goalH/2 && y < goalH/2) {
      if (x < -w/2) return -1;
      if (x > w/2) return 1;
    }
    return 0;
  }
}

void mousePressed() {
  if (ball.owner == 1) {
    ball.charging = true;
    ball.shootPower = 0;
  }
}


void mouseReleased() {
  if (ball.owner == 1) {
    float angle = atan2(
      mouseY + camera.y - player.y,
      mouseX + camera.x - player.x
    );
    ball.vx = cos(angle) * ball.shootPower;
    ball.vy = sin(angle) * ball.shootPower;
    ball.owner = 0;
    ball.charging = false;
    ball.shootPower = 0;
  }
}


void keyPressed() {
  if (key == 'w') player.up = true;
  if (key == 's') player.down = true;
  if (key == 'a') player.left = true;
  if (key == 'd') player.right = true;
}

void keyReleased() {
  if (key == 'w') player.up = false;
  if (key == 's') player.down = false;
  if (key == 'a') player.left = false;
  if (key == 'd') player.right = false;
}

void drawUI() {
  fill(255);
  textSize(32);
  textAlign(CENTER);
  text(leftScore + " : " + rightScore, width/2, 40);

  if (goalTimer > 0) {
    textSize(64);
    text("GOAL!", width/2, height/2);
    goalTimer--;
  }
}

class Enemy {
  float x, y;
  float vx, vy;

  float accel = 0.35;
  float friction = 0.9;
  float maxSpeed = 4.5;

  Enemy(float x, float y) {
    this.x = x;
    this.y = y;
  }

  void update(Ball ball, SoccerField field) {
    float tx, ty;

    // Decide target
    if (ball.owner == 2) {
      // Enemy has ball → go to left goal
      tx = -field.w/2 - 50;
      ty = 0;
    }
    else if (ball.owner == 1) {
      // Player has ball → chase player
      tx = player.x;
      ty = player.y;
    }
    else {
      // Free ball
      tx = ball.x;
      ty = ball.y;
    }


    // Move toward target
    float dx = tx - x;
    float dy = ty - y;
    float dist = sqrt(dx*dx + dy*dy);

    if (dist > 1) {
      dx /= dist;
      dy /= dist;
      vx += dx * accel;
      vy += dy * accel;
    } else {
      vx *= friction;
      vy *= friction;
    }

    // Cap speed
    float speed = sqrt(vx*vx + vy*vy);
    if (speed > maxSpeed) {
      vx = vx / speed * maxSpeed;
      vy = vy / speed * maxSpeed;
    }

    x += vx;
    y += vy;
    
    // Player steals from enemy
    if (ball.owner == 2 && dist(player.x, player.y, x, y) < 30) {
      ball.owner = 1;
    }

    // Steal from player
    if (ball.owner == 1 && dist(x, y, player.x, player.y) < 30) {
      ball.owner = 2;
    }
    
    // Pick up free ball
    if (ball.owner == 0 && dist(x, y, ball.x, ball.y) < 30) {
      ball.owner = 2;
    }

  }

  void draw() {
    fill(255, 80, 80);
    noStroke();
    ellipse(x, y, 30, 30);
  }
}
