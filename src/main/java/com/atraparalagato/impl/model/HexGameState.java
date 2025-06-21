package com.atraparalagato.impl.model;

import com.atraparalagato.base.model.GameState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementación de GameState para tableros hexagonales.
 * 
 * Los estudiantes pueden extender esta clase para agregar reglas o lógica adicional.
 */
public class HexGameState extends GameState<HexPosition> {

    private HexPosition catPosition;
    private final HexGameBoard gameBoard;
    private final int boardSize;

    // Constructor que inicializa el estado del juego con un ID y tamaño de tablero
    public HexGameState(String gameId, int boardSize) {
        super(gameId);
        this.boardSize = boardSize;
        this.gameBoard = new HexGameBoard(boardSize);
        this.catPosition = new HexPosition(0, 0); // Gato inicia en el centro
    }


    @Override
    protected boolean canExecuteMove(HexPosition position) {
        // Solo permite movimientos válidos y no bloqueados
        return gameBoard.isValidMove(position);
    }

    @Override
    protected boolean performMove(HexPosition position) {
        // Ejecuta el movimiento en el tablero
        if (canExecuteMove(position)) {
            gameBoard.executeMove(position);
            incrementMoveCount();
            updateGameStatus();
            return true;
        }
        return false;
    }

    @Override
    protected void updateGameStatus() {
        if (isCatAtBorder()) {
            setStatus(GameStatus.PLAYER_LOST); // El gato escapó
        } else if (isCatTrapped()) {
            setStatus(GameStatus.PLAYER_WON); // El gato está atrapado
        } else if (getMoveCount() >= boardSize * 3) {
            setStatus(GameStatus.DRAW); // Empate por demasiados movimientos
        }
    // Puedes agregar más condiciones aquí
    }

    @Override
    public HexPosition getCatPosition() {
        return catPosition;
    }

    @Override
    public void setCatPosition(HexPosition position) {
        this.catPosition = position;
        updateGameStatus();
    }

    @Override
    public boolean isGameFinished() {
        return getStatus() != GameStatus.IN_PROGRESS;
    }

    @Override
    public boolean hasPlayerWon() {
        return getStatus() == GameStatus.PLAYER_WON;
    }

    @Override
    public int calculateScore() {
        int baseScore = 0;
        if (hasPlayerWon()) {
            baseScore = 1000 + (boardSize * 50);
            // Bonificación por rapidez
            baseScore += Math.max(0, 500 - getMoveCount() * 15);
            // Bonificación si el gato está rodeado por más de 3 bloqueos
            List<HexPosition> adj = gameBoard.getAdjacentPositions(catPosition);
            long blockedAdj = adj.stream().filter(gameBoard::isBlocked).count();
            if (blockedAdj >= 4) baseScore += 100;
        } else if (getStatus() == GameStatus.DRAW) {
            baseScore = 200;
        } else {
            // Penalización por perder
            baseScore = Math.max(0, 100 - getMoveCount() * 5);
        }
        return baseScore;
    }

    
    @Override
    public Object getSerializableState() {
        Map<String, Object> state = new HashMap<>();
        state.put("gameId", getGameId());
        state.put("catPosition", Map.of("q", catPosition.getQ(), "r", catPosition.getR()));
        state.put("blockedCells", gameBoard.getBlockedPositions());
        state.put("status", getStatus().toString());
        state.put("moveCount", getMoveCount());
        state.put("boardSize", boardSize);
        return state;
    }


    @Override
    public void restoreFromSerializable(Object serializedState) {
    if (serializedState instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> state = (Map<String, Object>) serializedState;

        @SuppressWarnings("unchecked")
        Map<String, Integer> catPos = (Map<String, Integer>) state.get("catPosition");
        if (catPos != null) {
            this.catPosition = new HexPosition(catPos.get("q"), catPos.get("r"));
        }

        String statusStr = (String) state.get("status");
        if (statusStr != null) {
            setStatus(GameStatus.valueOf(statusStr));
        }

        // Restaurar celdas bloqueadas
        @SuppressWarnings("unchecked")
        Set<HexPosition> blocked = (Set<HexPosition>) state.get("blockedCells");
        if (blocked != null) {
            gameBoard.getBlockedPositions().clear();
            gameBoard.getBlockedPositions().addAll(blocked);
        }

        Integer moveCount = (Integer) state.get("moveCount");
        if (moveCount != null) {
        }
    }
}
    // Métodos auxiliares privados
    private boolean isCatAtBorder() {
        // El gato escapa si llega al borde
        return Math.abs(catPosition.getQ()) == boardSize ||
               Math.abs(catPosition.getR()) == boardSize ||
               Math.abs(catPosition.getS()) == boardSize;
    }

    private boolean isCatTrapped() {
        // El gato está atrapado si todas las posiciones adyacentes están bloqueadas
        List<HexPosition> adj = gameBoard.getAdjacentPositions(catPosition);
        return adj.isEmpty() || adj.stream().allMatch(gameBoard::isBlocked);
    }

    // Getter para el tablero
    public HexGameBoard getGameBoard() {
        return gameBoard;
    }

    public int getBoardSize() {
        return boardSize;
    }
}