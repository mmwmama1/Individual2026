package gui;

import game.Cavern.Direction;
import game.Node;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;

import static game.Cavern.Direction.*;
import static gui.Constants.SPRITESHEET;

/**
 * Responsible for managing the explorer and drawing it on the screen.
 * Handles functions to update the explorer and update its drawing as well.
 */
public class ExplorerSprite extends JPanel {
    private final Sprite sprite; // Sprite class to handle animating the explorer
    private final int tileWidth;
    private final int tileHeight;
    private final BlockingQueue<MovePair> queuedMoves;// List of moves we need to make to get to the goal location
    private final Semaphore blockUntilDone; // Allow our moveTo to block until complete.
    private final double ANIMATION_FPS = 10; // Number of animation frames displayed per second
    private int row; // Explorer's row index (updates only once move completes)
    private int col; // Explorer's column index (updates only once move completes)
    private int posX; // x-coordinate (pixels)
    private int posY; // y-coordinate(pixels)
    private Direction dir = NORTH; // Which direction is the explorer currently facing?

    /**
     * Constructor: an instance with player;'s starting position (startRow,
     * startCol).
     */
    public ExplorerSprite(int startRow, int startCol, int tileWidth, int tileHeight) {
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;

        // Initialize fields
        // Width (in pixels) of a single explorer image on the spritesheet
        int SPRITEWIDTH = 29;
        // Height (in pixels) if a single explorer image on the spritesheet
        int SPRITEHEIGHT = 36;
        sprite = new Sprite(SPRITESHEET, SPRITEWIDTH, SPRITEHEIGHT, 3);
        queuedMoves = new SynchronousQueue<MovePair>();
        blockUntilDone = new Semaphore(0);

        // Initialize our starting location
        row = startRow;
        col = startCol;
        posX = row * tileWidth;
        posY = col * tileHeight;

        // Create a thread which will periodically update the explorer's position
        // Move to the goal
        // Get the next move to make
        // Thread that updates explorer's location
        Thread updateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        int frames = GUI.getFramesPerMove();
                        MovePair move = queuedMoves.take();
                        // Move to the goal
                        for (int i = 1; i <= frames; i++) {
                            long startTime = System.currentTimeMillis();
                            // Get the next move to make
                            update(frames, i, move);
                            long lagTime = System.currentTimeMillis() - startTime;
                            if (lagTime < 1000 / GUI.getFramesPerSecond()) {
                                Thread.sleep(1000 / GUI.getFramesPerSecond() - lagTime);
                            }
                        }
                        blockUntilDone.release();

                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        });

        updateThread.start();

        // Create a thread that will periodically update the explorer's animation
        // Thread that updates explorer's animation
        Thread animationUpdateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        long startTime = System.currentTimeMillis();
                        sprite.tick();
                        long lagTime = System.currentTimeMillis() - startTime;
                        if (lagTime < 1000 / ANIMATION_FPS) {
                            Thread.sleep((long) (1000 / ANIMATION_FPS - lagTime));
                        }
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        });

        animationUpdateThread.start();
    }

    /**
     * Return the image representing the current state of the explorer.
     */
    public BufferedImage sprite(Direction dir) {
        /*
         * Use the direction to determine which offset into the spritesheet to use.
         * Class Sprite handles animation.
         */
        return switch (dir) {
            case SOUTH -> sprite.getSprite(0, 3);
            case WEST -> sprite.getSprite(1, 0);
            case EAST -> sprite.getSprite(1, 3);
            case NORTH -> sprite.getSprite(0, 0);
        };
    }

    /**
     * Return the explorer's row on the grid. Will remain the explorer's old
     * position until the explorer has completely arrived at the new one.
     */
    public int getRow() {
        return row;
    }

    /**
     * Return the explorer's column on the grid. Will remain the explorer's old
     * position until the explorer has completely arrived at the new one.
     */
    public int getCol() {
        return col;
    }

    /*
     * Tell the explorer to move from its current location to dst. After making
     * move, calling thread will block until move completes on GUI. Precondition:
     * dst must be adjacent to the current location and not currently moving. May
     * throw an InterruptedException
     */
    public void moveTo(Node dst) throws InterruptedException {
        dir = getDirection(row, col, dst.getTile().getRow(), dst.getTile().getColumn());

        // Determine sequence of moves to add to queue to get to goal
        int xDiff = (dst.getTile().getColumn() - col) * tileWidth;
        int yDiff = (dst.getTile().getRow() - row) * tileHeight;
        queuedMoves.put(new MovePair(xDiff, yDiff));

        blockUntilDone.acquire();
        row = dst.getTile().getRow();
        col = dst.getTile().getColumn();
    }

    /**
     * Draw the explorer on its own panel.
     */
    @Override
    public void paintComponent(Graphics page) {
        super.paintComponent(page);
        page.drawImage(sprite(dir), posX, posY, tileWidth, tileHeight, null);
    }

    /**
     * Update the location of the explorer as necessary.
     */
    private void update(int framesPerMove, int framesIntoMove, MovePair move) {
        // Make the move toward our destination
        posX = tileWidth * getCol() + (framesIntoMove * move.xDiff()) / framesPerMove;
        posY = tileHeight * getRow() + (framesIntoMove * move.yDiff()) / framesPerMove;
        repaint();
    }

    /**
     * Return the the direction the current location (row, col) to (goalRow,
     * goalCol). If already there, return the current direction.
     */
    private Direction getDirection(int row, int col, int goalRow, int goalCol) {
        if (goalRow < row) {
            return NORTH;
        }
        if (goalRow > row) {
            return SOUTH;
        }
        if (goalCol < col) {
            return WEST;
        }
        if (goalCol > col) {
            return EAST;
        }
        return dir;
    }
}