package gui;

import game.Cavern;
import game.Node;
import game.Tile;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;

import lombok.experimental.Accessors;
import lombok.Getter;
import lombok.Setter;

/**
 * An instance is a GUI for the game.
 */
//@Accessors(fluent = true)
public class GUI extends JFrame implements Constants {

    @Getter
    @Setter
    private static int framesPerSecond = 60;        // Framerate of game (fps)
    @Getter
    @Setter
    private static int framesPerMove = 25;          // How many frames does a single move take us?


//    private static final int FRAMESPERSECOND = 60;  // Framerate of game (fps)
//    private static final int FRAMESPERMOVE = 25;    // How many frames does a single move take us?
    private MazePanel mazePanel;                    //The panel for generating and drawing the maze
    private final ExplorerSprite explorer;          //The panel for updating and drawing the explorer
    private final OptionsPanel options;             //The panel for showing stats / displaying options
    private final TileSelectPanel tileSelect;       //Panel that provides more info on seleced tile


    /* Constructor a new display for cavern canvern with the player at (playerRow, playerCol)
     * using randomg number seed seed. */
    public GUI(Cavern cavern, int playerRow, int playerCol, long seed) {
        //Initialize frame
        setSize(SCREENWIDTH, SCREENHEIGHT);
        setLocation(150, 150);

        int gameWidth = (int) (GAMEWIDTH * SCREENWIDTH);
        int gameHeight = (int) (GAMEHEIGHT * SCREENHEIGHT);

        //Create the maze
        try {
            mazePanel = new MazePanel(cavern, gameWidth, gameHeight, this);
        }
        catch (Exception e){
            System.err.println(e);
            System.err.println("""
            Unable to create MazePanel for GUI.
            Application is unable to continue.""");
            System.exit(-1);
        }
        mazePanel.setBounds(0, 0, gameWidth, gameHeight);
        mazePanel.setVisited(playerRow, playerCol);

        //Create the explorer
        explorer = new ExplorerSprite(playerRow, playerCol, mazePanel.tileWidth, mazePanel.tileHeight);
        explorer.setBounds(0, 0, gameWidth, gameHeight);
        explorer.setOpaque(false);

        //Create the panel for stats and options
        options = new OptionsPanel(gameWidth, 0, SCREENWIDTH - gameWidth, (int) (SCREENHEIGHT * INFOSIZE), seed);

        //Create the panel for tile information
        tileSelect = new TileSelectPanel(gameWidth, (int) (SCREENHEIGHT * INFOSIZE),
            SCREENWIDTH - gameWidth, (int) (SCREENHEIGHT * (1 - INFOSIZE)), this);

        //Layer the explorer and maze into master panel
        //The panel that holds all other panels
        JLayeredPane master = new JLayeredPane();
        master.add(mazePanel, 1);
        master.add(options, 1);
        master.add(tileSelect, 1);
        master.add(explorer, 0);

        //Display GUI
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(master);
        setVisible(true);

        //What to do when the GUI resized?
        addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                int GAME_WIDTH = (int) (GAMEWIDTH * SCREENWIDTH);
                int GAME_HEIGHT = (int) (GAMEHEIGHT * SCREENHEIGHT);
                mazePanel.updateScreenSize(GAME_WIDTH, GAME_HEIGHT);
                mazePanel.setBounds(0, 0, GAME_WIDTH, GAME_HEIGHT);
                explorer.setBounds(0, 0, GAME_WIDTH, GAME_HEIGHT);
                explorer.repaint();
                options.setBounds(GAME_WIDTH, 0, SCREENWIDTH - GAME_WIDTH, (int) (SCREENHEIGHT * INFOSIZE));
                tileSelect.updateLoc(GAME_WIDTH, (int) (SCREENHEIGHT * INFOSIZE),
                    SCREENWIDTH - GAME_WIDTH, (int) (SCREENHEIGHT * (1 - INFOSIZE)));
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });
    }

    /**
     * Move the player on the GUI to destination dest.
     * Note : This blocks until the player has moved.
     * Precondition : dest is adjacent to the player's current location
     */
    public void moveTo(Node dest) {
        try {
            mazePanel.setVisited(dest.getTile().getRow(), dest.getTile().getColumn());
            explorer.moveTo(dest);
        } catch (InterruptedException e) {
            throw new RuntimeException("GUI moveTo : Must wait for move to finish");
        }
    }

    /**
     * Update the bonus multiplier as displayed by the GUI by bonus
     */
    public void updateBonus(double bonus) {
        options.updateBonus(bonus);
    }

    /**
     * Update the number of coins picked up as displayed on the GUI.
     *
     * @param coins the number of coins to be displayed
     * @param score the player's current score
     */
    public void updateCoins(int coins, int score) {
        options.updateCoins(coins, score);
        tileSelect.repaint();
    }

    /**
     * Update the time remaining as displayed on the GUI.
     * timeRemaining is the time remaining before the cave collapses
     */
    public void updateTimeRemaining(int timeRemaining) {
        options.updateTimeRemaining(timeRemaining);
    }

    /**
     * What is the specification?
     */
    public void updateCavern(Cavern c, int numStepsRemaining) {
        mazePanel.setCavern(c);
        options.updateMaxTimeRemaining(numStepsRemaining);
        updateTimeRemaining(numStepsRemaining);
        tileSelect.repaint();
    }

    /**
     * Set the cavern to be all light or all dark, depending on light.
     */
    public void setLighting(boolean light) {
        mazePanel.setLighting(light);
    }

    /**
     * Return an image representing tile type.
     */
    public BufferedImage getIcon(Tile.Type type) {
        return mazePanel.getIcon(type);
    }

    /**
     * Return an icon for the gold on tile n, or null otherwise.
     */
    public BufferedImage getGoldIcon(Node n) {
        return mazePanel.getGoldIcon(n);
    }

    /**
     * Select node n on the GUI. This displays information on that
     * node's panel on the screen to the right.
     */
    public void selectNode(Node n) {
        tileSelect.selectNode(n);
    }

    /**
     * Display error e to the player.
     */
    public void displayError(String e) {
        JFrame errorFrame = new JFrame();
        errorFrame.setTitle("Error in Solution");
        JLabel errorText = new JLabel(e);
        errorText.setHorizontalAlignment(JLabel.CENTER);
        errorFrame.add(errorText);
        errorFrame.setSize(ERRORWIDTH, ERRORHEIGHT);
        errorFrame.setLocation(new Point(getX() + getWidth() / 2 - ERRORWIDTH / 2,
            getY() + getHeight() / 2 - ERRORHEIGHT / 2));
        errorFrame.setVisible(true);
    }
}
