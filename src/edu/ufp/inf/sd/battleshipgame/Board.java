package edu.ufp.inf.sd.battleshipgame;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

/**
 * Author: Jeevan Opel
 * Decription: 	Board is a class which is a JPanel. The class has a 10x10 grid of JButtons, and draws them on
 * 				itself because it is a JPanel. The Board is designed to be added to JFrames. The Board is used
 * 				to represent the enemy grid, while its child class PBoard represents the player's grid and can
 * 				place ships.
 *
 * Method									Description
 * 
 * Board()									Class constructor. Initializes all the variables.
 * void drawBoard()							Sets the property of the JPanel, and adds the grid of JButtons along with
 * 											the row and column markers. The JPanel is an 11x11, the outer west and north lines 
 * 											are used for the column and row markers, while the inner 10x10 is used for the JButtons
 * void placeHitMarker(int row, int col)	Changes the color of a specified button to red, to represent that the cell was shot on by
 * 											the player and struck a ship. Also disables the button to prevent re-clicking
 * void placeMissMarker(int row, int col)	Changes the color of a specified button to black, representing a miss. Also disables the
 * 											button so the user cannot fire on it again
 * int getRclick()							Returns an integer representing the row of the button last clicked
 * int getCclick()							Returns an integer representing the column of the button last clicked
 * void actionPerformed(ActionEvent e)		When a button is clicked on an instance of Board, the action listener is activated
 * 											and finds which button in the grid was pressed. Then, it sets the rClick and cClick
 * 											to the corresponding row and column so it can be retrieved using their getters.
 * 
 */
public class Board extends JPanel {

	protected JButton btnGrid[][];
	private Border dborder;
	private JLabel markers[];

	public Board() {
		super();

		this.btnGrid = new JButton[10][10];
		dborder = new LineBorder(Color.GREEN, 1);
		markers = new JLabel[21];

		for (int i = 0; i < btnGrid.length; i++) {
			for (int j = 0; j < btnGrid.length; j++) {
				btnGrid[i][j] = new JButton();
				btnGrid[i][j].setText("");
				btnGrid[i][j].setBackground(Color.BLACK);
				btnGrid[i][j].setBorder(dborder);
			}
		}
		
		for (int i = 0; i < markers.length; i++) {
			markers[i] = new JLabel("");
			markers[i].setForeground(Color.GREEN);
			markers[i].setFont(new Font("Monospaced", Font.BOLD, 11));
			markers[i].setBackground(Color.BLACK);
			markers[i].setOpaque(true);
		}
	}
	
	//Sets the property of the JPanel, and adds the grid of JButtons along with
	//the row and column markers. The JPanel is an 11x11, the outer west and north lines 
	//are used for the column and row markers, while the inner 10x10 is used for the JButtons
	public void drawBoard() {
		
		setSize(400, 400);
		setLayout(new GridLayout(11, 11));
		setBounds(0, 0, 200, 200);
		setBackground(Color.BLACK);

		int index = 1;

		JLabel corner = new JLabel("");
		corner.setBackground(Color.BLACK);
		corner.setOpaque(true);
		add(corner);
		for (int i = 1; i < 11; i++) {
			markers[index].setText(Integer.toString(i));
			add(markers[index]);
			index++;
		}
		
		for (int i = 0; i < btnGrid.length; i++) {
			for (int j = 0; j < btnGrid.length; j++) {
				if (j == 0) {
					markers[index].setText(Character.toString((char)(65 + i)));
					add(markers[index]);
					index++;
				}
				add(btnGrid[i][j]);
			}
		}
		setVisible(true);
	}

	/**
	 * Self testing main method
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Board b = new Board();
		b.drawBoard();
		
	}
}
