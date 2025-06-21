package com.atraparalagato.base.model;

import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * Estado abstracto del juego.
 * 
 * Design Patterns:
 * - State Pattern: Maneja diferentes estados del juego
 * - Observer Pattern: Notifica cambios de estado
 * - Command Pattern: Encapsula acciones del juego
 * 
 * SOLID Principles:
 * - Single Responsibility: Maneja únicamente el estado del juego
 * - Open/Closed: Permite extensión de nuevos estados sin modificación
 * - Liskov Substitution: Las implementaciones deben ser intercambiables
 * 
 * Conceptos a implementar:
 * - Programación Funcional: Consumer, Supplier, callbacks
 * - Modularización: Separación de estado, lógica y persistencia
 * - OOP: Encapsulación del estado y comportamiento
 */
public abstract class GameState<T extends Position> {
    
    protected final String gameId;
    protected final LocalDateTime createdAt;
    protected GameStatus status;
    protected int moveCount;
    
    // Functional Programming: Callbacks para eventos
    protected Consumer<GameState<T>> onStateChanged;
    protected Consumer<GameState<T>> onGameEnded;
    
    protected GameState(String gameId) {
        this.gameId = gameId;
        this.createdAt = LocalDateTime.now();
        this.status = GameStatus.IN_PROGRESS;
        this.moveCount = 0;
    }
    
    /**
     * Ejecuta un movimiento en el juego.
     * Template Method que define el flujo general.
     */
    public final boolean executeMove(T position) {
        if (!canExecuteMove(position)) {
            return false;
        }
        
        beforeMoveExecution(position);
        boolean success = performMove(position);
        
        if (success) {
            incrementMoveCount();
            updateGameStatus();
            afterMoveExecution(position);
            notifyStateChanged();
        }
        
        return success;
    }
    
    /**
     * Verifica si un movimiento puede ser ejecutado.
     * Los estudiantes deben implementar las reglas específicas.
     */
    protected abstract boolean canExecuteMove(T position);
    
    /**
     * Realiza el movimiento específico en el estado del juego.
     * Aquí se actualiza el estado interno.
     */
    protected abstract boolean performMove(T position);
    
    /**
     * Actualiza el estado del juego después de un movimiento.
     * Determina si el juego continúa, termina, etc.
     */
    protected abstract void updateGameStatus();
    
    /**
     * Obtiene la posición actual del gato.
     * Fundamental para la lógica del juego.
     */
    public abstract T getCatPosition();
    
    /**
     * Establece la posición del gato.
     * Usado por los algoritmos de movimiento.
     */
    public abstract void setCatPosition(T position);
    
    /**
     * Verifica si el juego ha terminado.
     * Puede ser por victoria, derrota o empate.
     */
    public abstract boolean isGameFinished();
    
    /**
     * Determina si el jugador ha ganado.
     * Los estudiantes definen las condiciones de victoria.
     */
    public abstract boolean hasPlayerWon();
    
    /**
     * Calcula la puntuación actual del juego.
     * Los estudiantes pueden implementar diferentes sistemas de puntuación.
     */
    public abstract int calculateScore();
    
    /**
     * Obtiene una representación serializable del estado.
     * Útil para persistencia y comunicación con el frontend.
     */
    public abstract Object getSerializableState();
    
    /**
     * Restaura el estado desde una representación serializada.
     * Permite cargar juegos guardados.
     */
    public abstract void restoreFromSerializable(Object serializedState);
    
    // Hook methods para extensibilidad
    protected void beforeMoveExecution(T position) {
        // Default: no operation
    }
    
    protected void afterMoveExecution(T position) {
        // Default: no operation
    }
    
    // Functional Programming: Manejo de callbacks
    public void setOnStateChanged(Consumer<GameState<T>> callback) {
        this.onStateChanged = callback;
    }
    
    public void setOnGameEnded(Consumer<GameState<T>> callback) {
        this.onGameEnded = callback;
    }
    
    protected void notifyStateChanged() {
        if (onStateChanged != null) {
            onStateChanged.accept(this);
        }
        
        if (isGameFinished() && onGameEnded != null) {
            onGameEnded.accept(this);
        }
    }
    
    // Getters básicos
    public final String getGameId() {
        return gameId;
    }
    
    public final LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public final GameStatus getStatus() {
        return status;
    }
    
    public int getMoveCount() {
        return moveCount;
    }
    
    protected final void incrementMoveCount() {
        this.moveCount++;
    }
    
    protected final void setStatus(GameStatus status) {
        this.status = status;
    }
    
    /**
     * Enumeración de estados posibles del juego.
     */
    public enum GameStatus {
        IN_PROGRESS,
        PLAYER_WON,
        PLAYER_LOST,
        DRAW
    }
} 