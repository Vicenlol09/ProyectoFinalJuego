package com.atraparalagato.impl.model;

import com.atraparalagato.base.model.GameState;
import com.atraparalagato.base.strategy.CatMovementStrategy;

import java.util.*;


/**
 * Implementación de GameState para tableros hexagonales.
 * 
 * Los estudiantes pueden extender esta clase para agregar reglas o lógica adicional.
 */
public class HexGameState extends GameState<HexPosition> {

    private HexPosition catPosition;
    private final HexGameBoard gameBoard;
    private final int boardSize;

    // Compatibilidad con HexGameService
    private String difficulty;
    private CatMovementStrategy<HexPosition> catMovementStrategy;
    private boolean paused = false;
    private String winner;
    private String playerId;
    private final List<Map<String, Object>> moveHistory = new ArrayList<>();
    private Date createdAt = new Date();

    // Constructor que inicializa el estado del juego con un ID y tamaño de tablero
    public HexGameState(String gameId, int boardSize) {
        super(gameId);
        this.boardSize = boardSize;
        this.gameBoard = new HexGameBoard(boardSize);
        this.catPosition = new HexPosition(0, 0); // El gato inicia en el centro
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
        // Actualiza el estado del juego según la posición del gato y el número de movimientos
        if (isCatAtBorder()) {
            setStatus(GameStatus.PLAYER_LOST); // El gato escapó
            winner = "cat";
        } else if (isCatTrapped()) {
            setStatus(GameStatus.PLAYER_WON); // El gato está atrapado
            winner = playerId != null ? playerId : "player";
        } else if (getMoveCount() >= boardSize * 3) {
            setStatus(GameStatus.DRAW); // Empate por demasiados movimientos
            winner = "draw";
        }
        // Puedes agregar más condiciones aquí
    }

    @Override
    public HexPosition getCatPosition() {
        // Devuelve la posición actual del gato
        return catPosition;
    }

    @Override
    public void setCatPosition(HexPosition position) {
        // Establece la posición del gato y actualiza el estado del juego
        this.catPosition = position;
        updateGameStatus();
    }

    @Override
    public boolean isGameFinished() {
        // Indica si el juego ha terminado
        return getStatus() != GameStatus.IN_PROGRESS;
    }

    @Override
    public boolean hasPlayerWon() {
        // Indica si el jugador ha ganado
        return getStatus() == GameStatus.PLAYER_WON;
    }

    @Override
    public int calculateScore() {
        // Calcula la puntuación del jugador según el resultado y el número de movimientos
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
        // Devuelve el estado serializable del juego
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
        // Restaura el estado del juego a partir de un objeto serializado
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
        }
    }

    // Métodos auxiliares privados

    private boolean isCatAtBorder() {
        // El gato escapa si llega al borde del tablero
        return Math.abs(catPosition.getQ()) == boardSize ||
               Math.abs(catPosition.getR()) == boardSize ||
               Math.abs(catPosition.getS()) == boardSize;
    }

    private boolean isCatTrapped() {
        // El gato está atrapado si todas las posiciones adyacentes están bloqueadas
        List<HexPosition> adj = gameBoard.getAdjacentPositions(catPosition);
        return adj.isEmpty() || adj.stream().allMatch(gameBoard::isBlocked);
    }

    // Métodos y getters para compatibilidad con HexGameService

    public HexGameBoard getBoard() { return gameBoard; }
    public void setBoard(HexGameBoard board) { /* No-op, board es final */ }
    public int getBoardSize() { return boardSize; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public CatMovementStrategy<HexPosition> getCatMovementStrategy() { return catMovementStrategy; }
    public void setCatMovementStrategy(CatMovementStrategy<HexPosition> strategy) { this.catMovementStrategy = strategy; }

    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }

    public String getWinner() { return winner; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public int getScore() { return calculateScore(); }
    public Date getCreatedAt() { return createdAt; }

    // Historial de movimientos
    public void addMoveToHistory(HexPosition pos, String playerId) {
        // Agrega un movimiento al historial
        Map<String, Object> move = new HashMap<>();
        move.put("position", pos);
        move.put("playerId", playerId);
        move.put("moveNumber", getMoveCount());
        moveHistory.add(move);
    }
    public List<Map<String, Object>> getMoveHistory() { return moveHistory; }

    // Deshacer movimiento
    public boolean canUndo() { return !moveHistory.isEmpty(); }
    public void undoLastMove() {
        // Deshace el último movimiento realizado
        if (!moveHistory.isEmpty()) {
            Map<String, Object> last = moveHistory.remove(moveHistory.size() - 1);
            HexPosition pos = (HexPosition) last.get("position");
            gameBoard.getBlockedPositions().remove(pos);
            decrementMoveCount();
        }
    }

    // Decrementa el contador de movimientos (utilizado en undo)
    private void decrementMoveCount() {
        // Se asume que moveCount es gestionado en GameState y es protected
        if (getMoveCount() > 0) {
            // Si moveCount es private en GameState, puede ser necesario agregar un setter protegido o ajustar esto
            try {
                java.lang.reflect.Field moveCountField = GameState.class.getDeclaredField("moveCount");
                moveCountField.setAccessible(true);
                int current = (int) moveCountField.get(this);
                moveCountField.set(this, Math.max(0, current - 1));
            } catch (Exception e) {
                // Manejar excepción o registrar error
            }
        }
    }
}