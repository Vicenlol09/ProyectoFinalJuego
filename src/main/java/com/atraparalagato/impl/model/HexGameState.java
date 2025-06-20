package com.atraparalagato.impl.model;

import com.atraparalagato.base.model.GameState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación de GameState para tableros hexagonales.
 * 
 * Los estudiantes pueden extender esta clase para agregar reglas o lógica adicional.
 */
public class HexGameState extends GameState<HexPosition> {

    private HexPosition catPosition;
    private final HexGameBoard gameBoard;
    private final int boardSize;

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
        }
        // Si no, el juego sigue en progreso
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
        // Sistema de puntuación simple
        if (hasPlayerWon()) {
            return Math.max(0, 1000 - getMoveCount() * 10 + boardSize * 50);
        } else {
            return Math.max(0, 100 - getMoveCount() * 5);
        }
    }

    @Override
    public Object getSerializableState() {
        Map<String, Object> state = new HashMap<>();
        state.put("gameId", getGameId());
        state.put("catPosition", Map.of("q", catPosition.getQ(), "r", catPosition.getR()));
        state.put("blockedCells", gameBoard.blockedPositions);
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