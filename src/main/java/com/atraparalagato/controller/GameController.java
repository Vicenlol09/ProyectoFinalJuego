package com.atraparalagato.controller;

import com.atraparalagato.example.service.ExampleGameService;
import com.atraparalagato.impl.model.HexPosition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador del juego que alterna entre implementaciones de ejemplo y de estudiantes.
 * 
 * Usa la propiedad 'game.use-example-implementation' para determinar qué implementación usar:
 * - true: Usa las implementaciones del paquete 'example' (básicas, para guía)
 * - false: Usa las implementaciones del paquete 'impl' (de los estudiantes)
 */
@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameController {
    
    @Value("${game.use-example-implementation:false}")
    private boolean useExampleImplementation = false; // Por defecto, usa implementación de estudiantes
    
    private final ExampleGameService exampleGameService;
    
    public GameController() {
        this.exampleGameService = new ExampleGameService();
    }
    
    /**
     * Inicia un nuevo juego.
     */
    @GetMapping("/start")
    public ResponseEntity<Map<String, Object>> startGame(@RequestParam(defaultValue = "5") int boardSize) {
        try {
            if (useExampleImplementation) {
                return startGameWithExample(boardSize);
            } else {
                return startGameWithStudentImplementation(boardSize);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al iniciar el juego: " + e.getMessage()));
        }
    }
    
    /**
     * Ejecuta un movimiento del jugador.
     */
    @PostMapping("/block")
    public ResponseEntity<Map<String, Object>> blockPosition(
            @RequestParam String gameId,
            @RequestParam int q,
            @RequestParam int r) {
        try {
            HexPosition position = new HexPosition(q, r);
            
            if (useExampleImplementation) {
                return blockPositionWithExample(gameId, position);
            } else {
                return blockPositionWithStudentImplementation(gameId, position);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al ejecutar movimiento: " + e.getMessage()));
        }
    }
    
    /**
     * Obtiene el estado actual del juego.
     */
    @GetMapping("/state/{gameId}")
    public ResponseEntity<Map<String, Object>> getGameState(@PathVariable String gameId) {
        try {
            if (useExampleImplementation) {
                return getGameStateWithExample(gameId);
            } else {
                return getGameStateWithStudentImplementation(gameId);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener estado del juego: " + e.getMessage()));
        }
    }
    
    /**
     * Obtiene estadísticas del juego.
     */
    @GetMapping("/statistics/{gameId}")
    public ResponseEntity<Map<String, Object>> getGameStatistics(@PathVariable String gameId) {
        try {
            if (useExampleImplementation) {
                Map<String, Object> stats = exampleGameService.getGameStatistics(gameId);
                return ResponseEntity.ok(stats);
            } else {
                return ResponseEntity.ok(Map.of("error", "Student implementation not available yet"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener estadísticas: " + e.getMessage()));
        }
    }
    
    /**
     * Obtiene sugerencia de movimiento.
     */
    @GetMapping("/suggestion/{gameId}")
    public ResponseEntity<Map<String, Object>> getSuggestion(@PathVariable String gameId) {
        try {
            if (useExampleImplementation) {
                Optional<HexPosition> suggestion = exampleGameService.getSuggestedMove(gameId);
                if (suggestion.isPresent()) {
                    HexPosition pos = suggestion.get();
                    return ResponseEntity.ok(Map.of(
                        "suggestion", Map.of("q", pos.getQ(), "r", pos.getR()),
                        "message", "Sugerencia: bloquear posición adyacente al gato"
                    ));
                } else {
                    return ResponseEntity.ok(Map.of("message", "No hay sugerencias disponibles"));
                }
            } else {
                return ResponseEntity.ok(Map.of("error", "Student implementation not available yet"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener sugerencia: " + e.getMessage()));
        }
    }
    
    /**
     * Obtiene información sobre qué implementación se está usando.
     */
    @GetMapping("/implementation-info")
    public ResponseEntity<Map<String, Object>> getImplementationInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("useExampleImplementation", useExampleImplementation);
        info.put("currentImplementation", useExampleImplementation ? "example" : "impl");
        info.put("description", useExampleImplementation ? 
                "Usando implementaciones de ejemplo (básicas)" : 
                "Usando implementaciones de estudiantes");
        
        return ResponseEntity.ok(info);
    }
    
    // Métodos privados para implementación de ejemplo
    
    private ResponseEntity<Map<String, Object>> startGameWithExample(int boardSize) {
        var gameState = exampleGameService.startNewGame(boardSize);
        
        Map<String, Object> response = new HashMap<>();
        response.put("gameId", gameState.getGameId());
        response.put("status", gameState.getStatus().toString());
        response.put("catPosition", Map.of("q", gameState.getCatPosition().getQ(), "r", gameState.getCatPosition().getR()));
        response.put("blockedCells", gameState.getGameBoard().getBlockedPositions());
        response.put("movesCount", gameState.getMoveCount());
        response.put("boardSize", boardSize);
        response.put("implementation", "example");
        
        return ResponseEntity.ok(response);
    }
    
    private ResponseEntity<Map<String, Object>> blockPositionWithExample(String gameId, HexPosition position) {
        var gameStateOpt = exampleGameService.executePlayerMove(gameId, position);
        
        if (gameStateOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        var gameState = gameStateOpt.get();
        
        Map<String, Object> response = new HashMap<>();
        response.put("gameId", gameState.getGameId());
        response.put("status", gameState.getStatus().toString());
        response.put("catPosition", Map.of("q", gameState.getCatPosition().getQ(), "r", gameState.getCatPosition().getR()));
        response.put("blockedCells", gameState.getGameBoard().getBlockedPositions());
        response.put("movesCount", gameState.getMoveCount());
        response.put("implementation", "example");
        
        return ResponseEntity.ok(response);
    }
    
    private ResponseEntity<Map<String, Object>> getGameStateWithExample(String gameId) {
        var gameStateOpt = exampleGameService.getGameState(gameId);
        
        if (gameStateOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        var gameState = gameStateOpt.get();
        
        Map<String, Object> response = new HashMap<>();
        response.put("gameId", gameState.getGameId());
        response.put("status", gameState.getStatus().toString());
        response.put("catPosition", Map.of("q", gameState.getCatPosition().getQ(), "r", gameState.getCatPosition().getR()));
        response.put("blockedCells", gameState.getGameBoard().getBlockedPositions());
        response.put("movesCount", gameState.getMoveCount());
        response.put("implementation", "example");
        
        return ResponseEntity.ok(response);
    }
    
    // Métodos privados para implementación de estudiantes (placeholder)
    
    private ResponseEntity<Map<String, Object>> startGameWithStudentImplementation(int boardSize) {
        return ResponseEntity.ok(Map.of(
            "error", "Student implementation not available yet",
            "message", "Los estudiantes deben completar sus implementaciones en el paquete 'impl'",
            "implementation", "impl"
        ));
    }
    
    private ResponseEntity<Map<String, Object>> blockPositionWithStudentImplementation(String gameId, HexPosition position) {
        return ResponseEntity.ok(Map.of(
            "error", "Student implementation not available yet",
            "message", "Los estudiantes deben completar sus implementaciones en el paquete 'impl'",
            "implementation", "impl"
        ));
    }
    
    private ResponseEntity<Map<String, Object>> getGameStateWithStudentImplementation(String gameId) {
        return ResponseEntity.ok(Map.of(
            "error", "Student implementation not available yet",
            "message", "Los estudiantes deben completar sus implementaciones en el paquete 'impl'",
            "implementation", "impl"
        ));
    }
} 