package com.atraparalagato.impl.service;

import com.atraparalagato.base.service.GameService;
import com.atraparalagato.base.model.GameState;
import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.base.strategy.CatMovementStrategy;
import com.atraparalagato.impl.model.HexPosition;
import com.atraparalagato.impl.model.HexGameState;
import com.atraparalagato.impl.model.HexGameBoard;
import com.atraparalagato.impl.strategy.BFSCatMovement;
import com.atraparalagato.impl.strategy.AStarCatMovement;
import com.atraparalagato.impl.repository.H2GameRepository;

import java.util.*;
import java.util.function.Supplier;

/**
 * Implementación esqueleto de GameService para el juego hexagonal.
 * 
 * Los estudiantes deben completar los métodos marcados con TODO.
 * 
 * Conceptos a implementar:
 * - Orquestación de todos los componentes del juego
 * - Lógica de negocio compleja
 * - Manejo de eventos y callbacks
 * - Validaciones avanzadas
 * - Integración con repositorio y estrategias
 */
public class HexGameService extends GameService<HexPosition> {

    private final H2GameRepository gameRepository;
    private final Supplier<String> gameIdGenerator;

    public HexGameService() {
        this(new H2GameRepository(), () -> UUID.randomUUID().toString());
    }

    public HexGameService(H2GameRepository repository, Supplier<String> idGenerator) {
        super();
        this.gameRepository = repository;
        this.gameIdGenerator = idGenerator;
    }

    /**
     * TODO: Crear un nuevo juego con configuración personalizada.
     * Debe ser más sofisticado que ExampleGameService.
     */
    public HexGameState createGame(int boardSize, String difficulty, Map<String, Object> options) {
        if (boardSize < 3) throw new IllegalArgumentException("El tamaño mínimo es 3");
        String gameId = gameIdGenerator.get();
        HexGameBoard board = new HexGameBoard(boardSize);
        CatMovementStrategy<HexPosition> strategy = createMovementStrategy(difficulty, board);
        HexGameState gameState = new HexGameState(gameId, boardSize);
        gameState.setBoard(board);
        gameState.setDifficulty(difficulty);
        gameState.setCatMovementStrategy(strategy);

        // Callbacks y eventos
        gameState.setOnStateChanged(this::onGameStateChanged);
        gameState.setOnGameEnded(this::onGameEnded);

        gameRepository.save(gameState);
        return gameState;
    }

    /**
     * TODO: Ejecutar movimiento del jugador con validaciones avanzadas.
     */
    public Optional<HexGameState> executePlayerMove(String gameId, HexPosition position, String playerId) {
        Optional<HexGameState> optState = gameRepository.findById(gameId);
        if (optState.isEmpty()) return Optional.empty();
        HexGameState state = optState.get();

        if (state.isGameFinished()) return Optional.of(state);
        if (!isValidAdvancedMove(state, position, playerId)) return Optional.of(state);

        if (!state.getBoard().isPositionInBounds(position) || state.getBoard().isBlocked(position)) {
            return Optional.of(state);
        }
        state.getBoard().blockPosition(position);
        state.addMoveToHistory(position, playerId);

        // Mover el gato usando la estrategia
        executeCatMove(state, state.getDifficulty());

        // Actualizar y guardar
        gameRepository.save(state);

        // Notificar evento
        notifyGameEvent(gameId, "playerMove", Map.of("position", position, "playerId", playerId));
        return Optional.of(state);
    }

    /**
     * TODO: Obtener estado del juego con información enriquecida.
     */
    public Optional<Map<String, Object>> getEnrichedGameState(String gameId) {
        return gameRepository.findById(gameId).map(state -> {
            Map<String, Object> map = new HashMap<>();
            map.put("gameState", state);
            map.put("statistics", getGameStatistics(gameId));
            map.put("suggestedMove", getSuggestedMove(gameId).orElse(null));
            map.put("board", state.getBoard());
            map.put("moveHistory", state.getMoveHistory());
            return map;
        });
    }

    /**
     * TODO: Obtener sugerencia inteligente de movimiento.
     */
    public Optional<HexPosition> getIntelligentSuggestion(String gameId, String difficulty) {
        Optional<HexGameState> optState = gameRepository.findById(gameId);
        if (optState.isEmpty()) return Optional.empty();
        HexGameState state = optState.get();
        CatMovementStrategy<HexPosition> strategy = createMovementStrategy(difficulty, state.getBoard());
        List<HexPosition> candidates = state.getBoard().getAllAvailablePositions();
        HexPosition catPos = state.getCatPosition();
        // Sugerir la posición que maximice la distancia del gato al borde
        // Usar la distancia mínima al borde como heurística alternativa
        return candidates.stream()
                .max(Comparator.comparing(pos -> 
                    state.getBoard().getAllBorderPositions().stream()
                        .mapToDouble(border -> pos.distanceTo(border))
                        .min()
                        .orElse(Double.MAX_VALUE)
                ));
    }

    /**
     * TODO: Analizar la partida y generar reporte.
     */
    public Map<String, Object> analyzeGame(String gameId) {
        Optional<HexGameState> optState = gameRepository.findById(gameId);
        Map<String, Object> analysis = new HashMap<>();
        optState.ifPresent(state -> {
            analysis.put("moves", state.getMoveHistory());
            analysis.put("winner", state.getWinner());
            analysis.put("totalMoves", state.getMoveHistory().size());
            analysis.put("difficulty", state.getDifficulty());
        });
        return analysis;
    }

    /**
     * TODO: Obtener estadísticas globales del jugador.
     */
    public Map<String, Object> getPlayerStatistics(String playerId) {
        long total = gameRepository.countWhere(g -> g.getPlayerId().equals(playerId));
        long won = gameRepository.countWhere(g -> g.getPlayerId().equals(playerId) && g.hasPlayerWon());
        Map<String, Object> stats = new HashMap<>();
        stats.put("gamesPlayed", total);
        stats.put("gamesWon", won);
        stats.put("winRate", total > 0 ? (double) won / total * 100 : 0);
        return stats;
    }

    /**
     * TODO: Configurar dificultad del juego.
     */
    public void setGameDifficulty(String gameId, String difficulty) {
        gameRepository.findById(gameId).ifPresent(state -> {
            state.setDifficulty(difficulty);
            state.setCatMovementStrategy(createMovementStrategy(difficulty, state.getBoard()));
            gameRepository.save(state);
        });
    }

    /**
     * TODO: Pausar/reanudar juego.
     */
    public boolean toggleGamePause(String gameId) {
        Optional<HexGameState> optState = gameRepository.findById(gameId);
        if (optState.isEmpty()) return false;
        HexGameState state = optState.get();
        state.setPaused(!state.isPaused());
        gameRepository.save(state);
        notifyGameEvent(gameId, "pauseToggled", Map.of("paused", state.isPaused()));
        return state.isPaused();
    }

    /**
     * TODO: Deshacer último movimiento.
     */
    public Optional<HexGameState> undoLastMove(String gameId) {
        Optional<HexGameState> optState = gameRepository.findById(gameId);
        if (optState.isEmpty()) return Optional.empty();
        HexGameState state = optState.get();
        if (!state.canUndo()) return Optional.of(state);
        state.undoLastMove();
        gameRepository.save(state);
        notifyGameEvent(gameId, "undo", Map.of());
        return Optional.of(state);
    }

    /**
     * TODO: Obtener ranking de mejores puntuaciones.
     */
    public List<Map<String, Object>> getLeaderboard(int limit) {
        return gameRepository.findAllSorted(HexGameState::getScore, false).stream()
                .limit(limit)
                .map(state -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("playerId", state.getPlayerId());
                    entry.put("score", state.getScore());
                    entry.put("date", state.getCreatedAt());
                    entry.put("moves", state.getMoveHistory().size());
                    return entry;
                }).toList();
    }

    // Métodos auxiliares que los estudiantes pueden implementar
    
    /**
     * TODO: Validar movimiento según reglas avanzadas.
     */
    private boolean isValidAdvancedMove(HexGameState gameState, HexPosition position, String playerId) {
        return !gameState.getBoard().isBlocked(position)
                && !gameState.isGameFinished()
                && !position.equals(gameState.getCatPosition());
    }
    
    /**
     * TODO: Ejecutar movimiento del gato usando estrategia apropiada.
     */
    private void executeCatMove(HexGameState gameState, String difficulty) {
        CatMovementStrategy<HexPosition> strategy = gameState.getCatMovementStrategy();
        HexPosition catPos = gameState.getCatPosition();
        Optional<HexPosition> move = strategy.selectBestMove(
                strategy.getPossibleMoves(catPos),
                catPos,
                getTargetPosition(gameState)
        );
        move.ifPresent(gameState::setCatPosition);
    }
    
    /**
     * TODO: Calcular puntuación avanzada.
     */
    private int calculateAdvancedScore(HexGameState gameState, Map<String, Object> factors) {
        int base = gameState.getMoveHistory().size();
        int diff = "hard".equalsIgnoreCase(gameState.getDifficulty()) ? 10 : 0;
        return base + diff;
    }
    
    /**
     * TODO: Notificar eventos del juego.
     */
    private void notifyGameEvent(String gameId, String eventType, Map<String, Object> eventData) {
        System.out.printf("Evento [%s] en juego %s: %s%n", eventType, gameId, eventData);
    }
    
    /**
     * TODO: Crear factory de estrategias según dificultad.
     */
    private CatMovementStrategy<HexPosition> createMovementStrategy(String difficulty, HexGameBoard board) {
        if ("hard".equalsIgnoreCase(difficulty)) {
            return new AStarCatMovement(board);
        } else {
            return new BFSCatMovement(board);
        }
    }

    // Callbacks para eventos del juego
    private void onGameStateChanged(GameState<HexPosition> gameState) {
        System.out.println("📊 Estado del juego actualizado: " + gameState.getStatus());
    }

    private void onGameEnded(GameState<HexPosition> gameState) {
        String result = gameState.hasPlayerWon() ? "¡VICTORIA!" : "Derrota";
        System.out.println("🏁 Juego terminado: " + result + " - Puntuación: " + gameState.calculateScore());
    }

    // Métodos abstractos requeridos por GameService
    
    @Override
    protected void initializeGame(GameState<HexPosition> gameState, GameBoard<HexPosition> gameBoard) {
        gameState.setBoard(gameBoard);
    }
    
    @Override
    public boolean isValidMove(String gameId, HexPosition position) {
        Optional<HexGameState> optState = gameRepository.findById(gameId);
        return optState.filter(state -> !state.getBoard().isBlocked(position)).isPresent();
    }
    
    @Override
    public Optional<HexPosition> getSuggestedMove(String gameId) {
        Optional<HexGameState> optState = gameRepository.findById(gameId);
        if (optState.isEmpty()) return Optional.empty();
        HexGameState state = optState.get();
        CatMovementStrategy<HexPosition> strategy = state.getCatMovementStrategy();
        HexPosition catPos = state.getCatPosition();
        return strategy.getPossibleMoves(catPos).stream().findFirst();
    }
    
    @Override
    protected HexPosition getTargetPosition(GameState<HexPosition> gameState) {
        HexGameBoard board = (HexGameBoard) gameState.getBoard();
        HexPosition catPos = ((HexGameState) gameState).getCatPosition();
        return board.getAllBorderPositions().stream()
                .min(Comparator.comparingInt(catPos::distanceTo))
                .orElse(catPos);
    }
    
    @Override
    public Object getGameStatistics(String gameId) {
        return gameRepository.findById(gameId)
                .map(state -> state.getSerializableState())
                .orElse(null);
    }
}