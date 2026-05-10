package edu.ufp.inf.sd.battleshipgame.rmi;

import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import static org.junit.jupiter.api.Assertions.*;

public class R3GameLogicTest {

    private static class NoopObserver extends UnicastRemoteObject implements BattleshipGameObserver {
        protected NoopObserver() throws RemoteException {
            super();
        }

        @Override
        public void update(GameState state) throws RemoteException {
            // no-op
        }
    }

    @Test
    void gameLifecycle_twoPlayers_placeShips_thenStart() throws Exception {
        BattleshipGameSubjectImpl game = new BattleshipGameSubjectImpl("Game-TEST", false);

        game.attach("p1", new NoopObserver());
        GameState s1 = game.getState();
        assertEquals(GamePhase.WAITING_FOR_PLAYERS, s1.getPhase());
        assertEquals("p1", s1.getCurrentTurn());
        assertEquals(1, game.getPlayerCount());

        game.attach("p2", new NoopObserver());
        GameState s2 = game.getState();
        assertEquals(GamePhase.PLACING_SHIPS, s2.getPhase());
        assertEquals("p1", s2.getCurrentTurn());
        assertEquals(2, game.getPlayerCount());

        // p2 não pode jogar fora do turno
        assertThrows(RemoteException.class, () ->
                game.setState(GameAction.placeShip("p2",
                        new ShipPlacement(new Coordinate(0, 0), 5, Orientation.HORIZONTAL))));

        // p1 coloca 5 navios (5,4,3,3,2)
        placeShip(game, "p1", 0, 0, 5);
        placeShip(game, "p1", 1, 0, 4);
        placeShip(game, "p1", 2, 0, 3);
        placeShip(game, "p1", 3, 0, 3);
        placeShip(game, "p1", 4, 0, 2);

        assertEquals("p2", game.getState().getCurrentTurn());

        // p2 coloca 5 navios
        placeShip(game, "p2", 0, 0, 5);
        placeShip(game, "p2", 1, 0, 4);
        placeShip(game, "p2", 2, 0, 3);
        placeShip(game, "p2", 3, 0, 3);
        placeShip(game, "p2", 4, 0, 2);

        GameState s3 = game.getState();
        assertEquals(GamePhase.IN_PROGRESS, s3.getPhase());
        assertEquals("p1", s3.getCurrentTurn());
    }

    private static void placeShip(BattleshipGameSubject game, String user, int row, int col, int len) throws RemoteException {
        game.setState(GameAction.placeShip(user,
                new ShipPlacement(new Coordinate(row, col), len, Orientation.HORIZONTAL)));
    }
}

