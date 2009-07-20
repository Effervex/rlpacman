package rlPacMan;
import java.awt.*;

class Player extends Thing
{
   static int MAX_MOUTH_DEGREE = 60;
   int      m_degreeRotation = 0;      // Used to track Pacman's degree rotation
   int      m_score = 0;         
   int      m_mouthDegree = 45;        // Used to animate chomping
   boolean  m_mouthChomping = true;
   boolean  m_bDrawDead     = false;
   byte     m_requestedDirection = STILL;
   Rectangle   m_boundingBoxFull; // Full Bounding Box of Pacman.  This is different from the base class
                                  // box because it isn't adjusted due to Pacman being a circle and not a sqaure
   
   int      m_rotationDying      = 0;   // Used to animate Pacman dying
   int      m_mouthDegreeDying   = 45;
   int      m_mouthArcDying      = 135;   // Used to animate Pacman dying
   
   
   // This constructor is used to place Pacman's X-location between two cells.
   Player (GameModel gameModel, byte type, int startX, int startY, boolean bMiddleX)
   {
      super (gameModel, type, startX, startY, bMiddleX);
      m_boundingBoxFull = new Rectangle ();
   }
   
   // Called to check if Player can eat itemType from it's current position
   // lookAhead check is also used because the Player's bounding box is larger than
   // CELL_LENGTH and extends into other gamestate cells
   public void eatItem (int itemType)
   {
      GameUI gameUI = m_gameModel.m_pacMan.m_gameUI;
      Rectangle itemBoundingBox;
      Rectangle intersectRect;
      double itemPixelX;
      double itemPixelY;
      int   lookAheadX = m_locX;
      int   lookAheadY = m_locY;
      int   itemX = -1;
      int   itemY = -1;
         
      if (m_direction == LEFT && m_locX != 0)
         lookAheadX--;
      else if (m_direction == UP && m_locY != 0)
         lookAheadY--;
      else if (m_direction == RIGHT && m_locX != m_gameModel.m_gameSizeX - 1)
         lookAheadX++;
      else if (m_direction == DOWN && m_locY != m_gameModel.m_gameSizeY - 1)
         lookAheadY++;
         
      if ((m_gameModel.m_gameState[m_locX][m_locY] & itemType) != 0)
      {  
         itemX = m_locX;
         itemY = m_locY;
      } else if ((m_gameModel.m_gameState[lookAheadX][lookAheadY] & itemType) != 0)
      {
         itemX = lookAheadX;
         itemY = lookAheadY;
      }
      
      if (itemX != -1 && itemY != -1)
      {
         itemPixelX = gameUI.m_gridInset + itemX * gameUI.CELL_LENGTH;
         itemPixelY = gameUI.m_gridInset + itemY * gameUI.CELL_LENGTH;
         if (itemType != GameModel.GS_POWERUP)
            itemBoundingBox = new Rectangle ((int)itemPixelX + gameUI.WALL2, (int)itemPixelY + gameUI.WALL2, gameUI.WALL1, gameUI.WALL1);
         else
            itemBoundingBox = new Rectangle ((int)itemPixelX, (int)itemPixelY, gameUI.CELL_LENGTH, gameUI.CELL_LENGTH);
         intersectRect = m_boundingBoxFull.intersection (itemBoundingBox);
         if (!intersectRect.isEmpty ())
         {
            m_gameModel.m_currentFoodCount++;
            if (itemType != GameModel.GS_POWERUP)
            {
               m_score += 10;
               //m_gameModel.m_pacMan.m_soundMgr.playSound (SoundManager.SOUND_CHOMP);
            } else {
               m_score += 40;
               m_gameModel.eatPowerup ();
               //m_gameModel.m_pacMan.m_soundMgr.stopSound (SoundManager.SOUND_SIREN);
               //m_gameModel.m_pacMan.m_soundMgr.playSound (SoundManager.SOUND_GHOSTBLUE);
            }
            m_gameModel.m_gameState[itemX][itemY] &= ~itemType;  
         }
      }
   }
   
   // Overriden to draw Pacman
   public void draw (GameUI gameUI, Graphics g2)   {      if (!m_bVisible)         return;            
      int pacManDiameter = gameUI.CELL_LENGTH + gameUI.WALL1 + gameUI.WALL1;           
      double pacManX = gameUI.m_gridInset + m_locX * gameUI.CELL_LENGTH - pacManDiameter / 2.0;
      double pacManY = gameUI.m_gridInset + m_locY * gameUI.CELL_LENGTH - pacManDiameter / 2.0;
      double deltaPixelX = 0;
      double deltaPixelY = 0;
                 pacManX += gameUI.CELL_LENGTH / 2.0;
      pacManY += gameUI.CELL_LENGTH / 2.0;
      
      if (m_deltaLocX != 0)
         deltaPixelX = m_deltaLocX * (gameUI.CELL_LENGTH / (m_deltaMax * 2.0 - 1));
      else if (m_deltaLocY != 0)
         deltaPixelY = m_deltaLocY * (gameUI.CELL_LENGTH / (m_deltaMax * 2.0 - 1));
            if (m_direction != STILL && !m_bPaused)      {
         if (m_mouthChomping)            m_mouthDegree -= 20;         else            m_mouthDegree += 20;                  if (m_mouthDegree <= 0 || m_mouthDegree >= MAX_MOUTH_DEGREE)            m_mouthChomping = !m_mouthChomping;
      }
         
      switch (m_direction)
      {
         case Thing.UP:
            m_degreeRotation = 90;
            break;         case Thing.RIGHT:
            m_degreeRotation = 0;
            break;         case Thing.LEFT:
            m_degreeRotation = 180;
            break;         case Thing.DOWN:
            m_degreeRotation = 270;
            break;
      }
            g2.setColor (Color.yellow);      
      // Draw Pacman Chomping      if (!m_bDrawDead)      {
         g2.fillArc ((int)(pacManX + deltaPixelX), (int)(pacManY + deltaPixelY), pacManDiameter, pacManDiameter, m_degreeRotation + m_mouthDegree, 200);
         g2.fillArc ((int)(pacManX + deltaPixelX), (int)(pacManY + deltaPixelY), pacManDiameter, pacManDiameter, m_degreeRotation - m_mouthDegree, -200);      
      } else {         // Draw Pacman dying         if (m_rotationDying > 450)         {
            m_rotationDying = 450;            m_mouthDegreeDying += 5;
            m_mouthArcDying -= 5;
                        if (m_mouthArcDying < 0)
               m_mouthArcDying = 0;            
         }
         g2.fillArc ((int)(pacManX + deltaPixelX), (int)(pacManY + deltaPixelY), pacManDiameter, pacManDiameter, m_rotationDying + m_mouthDegreeDying, m_mouthArcDying);
         g2.fillArc ((int)(pacManX + deltaPixelX), (int)(pacManY + deltaPixelY), pacManDiameter, pacManDiameter, m_rotationDying - m_mouthDegreeDying, -m_mouthArcDying);
         m_rotationDying += 20;      }         
      m_boundingBox.setBounds ((int)(pacManX + deltaPixelX), (int)(pacManY + deltaPixelY), pacManDiameter, pacManDiameter);
      m_boundingBoxFull.setBounds (m_boundingBox);      m_boundingBox.grow (-pacManDiameter / 5, -pacManDiameter / 5); // Make the bounding box smaller because Pacman is a Circle and not a Square.
      
      // TODO: Draw bounding box for testing
      //g2.setColor (Color.white);      //g2.drawRect (m_boundingBox.x, m_boundingBox.y, m_boundingBox.width, m_boundingBox.height);   }
   
   // Overriden to update Pacman's direction
   public void tickThing ()
   {
      if (m_direction == m_requestedDirection)
         return;
      
      // See if we can make a 90 degree turn, this can only happen when the      // thing is located dead-center in the cell.
      if (m_deltaLocX == 0 && m_deltaLocY == 0)
      {         // Try to make a 90 degree turn left or right
         if ((m_direction == UP || m_direction == DOWN || m_direction == STILL) &&             (m_requestedDirection == LEFT || m_requestedDirection == RIGHT))
         {            // You can make a left turn if there is no wall there.
            if (m_requestedDirection == LEFT && (m_gameModel.m_gameState [m_locX][m_locY] & GameModel.GS_WEST) == 0) 
               m_direction = LEFT;
            else if (m_requestedDirection == RIGHT && (m_gameModel.m_gameState [m_locX][m_locY] & GameModel.GS_EAST) == 0) // Otherwise, try to make a right turn if there is no wall               m_direction = RIGHT;                  
         } else if ((m_direction == LEFT || m_direction == RIGHT || m_direction == STILL) &&  // Try to make a 90 degree turn up or down             (m_requestedDirection == UP || m_requestedDirection == DOWN))         {
            // You can turn up if there is no wall there.            if (m_requestedDirection == UP && (m_gameModel.m_gameState [m_locX][m_locY] & GameModel.GS_NORTH) == 0)                m_direction = UP;
            else if (m_requestedDirection == DOWN && (m_gameModel.m_gameState [m_locX][m_locY] & GameModel.GS_SOUTH) == 0) // Otherwise, try to make a down turn if there is no wall               m_direction = DOWN;         }      }
             
      // Direction change is also possible if the thing makes
      // a 180 degree turn.
      if ((m_direction == LEFT && m_requestedDirection == RIGHT) ||
          (m_direction == RIGHT && m_requestedDirection == LEFT) ||
          (m_direction == UP && m_requestedDirection == DOWN) ||
          (m_direction == DOWN && m_requestedDirection == UP))
      {
         m_direction = m_requestedDirection;
      }
      
      // In case Pacman is STILL and his deltaX or deltaY != 0,
      // then allow him to move that delta's direction.  
      // Ex. When Pacman starts, he's in between cells
      if (m_direction == STILL && m_deltaLocX != 0 &&
         (m_requestedDirection == RIGHT || m_requestedDirection == LEFT))
         m_direction = m_requestedDirection;
      else if (m_direction == STILL && m_deltaLocY != 0 &&
              (m_requestedDirection == UP || m_requestedDirection == DOWN))
         m_direction = m_requestedDirection;
   }
   
   public void returnToStart ()
   {
      super.returnToStart ();
      m_degreeRotation = 0;
      m_mouthDegree = 45;  
      m_mouthChomping = true;
      m_bDrawDead = false;
      m_requestedDirection = RIGHT;
      m_lastLocX = m_startX;
      m_lastLocY = m_startY;
      m_boundingBoxFull.setBounds (0, 0, 0, 0);
   }
}