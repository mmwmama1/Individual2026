package gui;

public interface Constants {
    double GAMEWIDTH = 0.78;       //Width of the game portion (prop of total)
    double GAMEHEIGHT = 1.0;      //Height of the game portion (prop of total)

    //Dimensions of the error pane (in pixels)
    int ERRORWIDTH = 500;
    int ERRORHEIGHT = 150;
    double INFOSIZE = 0.5;    //How much of the screen should the info make up?

    int SCREENWIDTH = 1050;    // Width of the entire screen
    int SCREENHEIGHT = 600;    // Height of the entire screen

    String ORB_PATH = "orb.png";           //Path to orb image
    String PATH_PATH = "path.png";         //Path to image representing path
    String WALL_PATH = "wall.png";         //Path to wall image
    String COIN_PATH = "coins.png";        //Path to the coin image
    String ENTRANCE_PATH = "entrance.png"; //Path to the entrance image
    String TASTY_PATH = "notes.png";
    String BACKGROUND_PATH = "info_texture.png";
    String SPRITESHEET = "explorer_sprites.png"; // Location of the spritesheet image

    int COIN_SPRITES_PER_ROW = 7;
    int COIN_SPRITES_PER_COL = 2;

}
