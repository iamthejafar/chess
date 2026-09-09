# Chess AI Feature Documentation

## Overview

This chess application now includes an advanced AI opponent system powered by the **Minimax algorithm with Alpha-Beta Pruning**. Players can challenge the computer at three difficulty levels: Easy, Medium, and Hard.

## Features

### 1. Minimax Algorithm with Alpha-Beta Pruning
- **Efficient Tree Search**: Uses alpha-beta pruning to dramatically reduce the number of nodes evaluated, making deep searches practical
- **Configurable Depth**: Different difficulty levels determine search depth
  - **EASY**: Depth 1, 500ms max time
  - **MEDIUM**: Depth 3, 2000ms max time
  - **HARD**: Depth 5, 5000ms max time

### 2. Position Evaluation
The AI evaluates board positions using:
- **Material Count**: Traditional piece values (Pawn=1, Knight=3, Bishop=3, Rook=5, Queen=9)
- **Center Control**: Bonus for controlling central squares (d4, e4, d5, e5)
- **King Safety**: Penalties for exposed kings, bonuses for attacking opponent's king
- **Piece Mobility**: Encourages moves that provide more legal move options

### 3. Game Modes

#### Human vs Computer
Players can initiate AI games with their desired difficulty:

```json
{
  "type": "INIT_GAME",
  "userId": "player-id",
  "gameType": "HUMAN_VS_COMPUTER",
  "difficulty": "MEDIUM"
}
```

#### Human vs Human (Standard)
Traditional multiplayer games continue to work as before.

## Implementation Details

### Core Components

#### 1. **AiService** (`AiService.java`)
- Location: `src/main/java/com/jafar/chess/service/`
- **Key Methods**:
  - `findBestMove(Board, Difficulty)`: Returns the best move for the current position
  - `minimax(Board, depth, alpha, beta, isMaximizing)`: Core minimax implementation
  - `evaluatePosition(Board)`: Evaluates board state numerically

#### 2. **Difficulty Enum** (`Difficulty.java`)
- Location: `src/main/java/com/jafar/chess/shared/`
- Defines AI difficulty levels with configurable depth and time limits:
  ```java
  EASY(1, 500),       // 1-ply search, 500ms timeout
  MEDIUM(3, 2000),    // 3-ply search, 2s timeout
  HARD(5, 5000)       // 5-ply search, 5s timeout
  ```

#### 3. **GameType Enum** (`GameType.java`)
- Location: `src/main/java/com/jafar/chess/shared/`
- Distinguishes between game types:
  - `HUMAN_VS_HUMAN`
  - `HUMAN_VS_COMPUTER`

#### 4. **Extended Game Model**
- New fields in `Game.java`:
  - `isComputerGame`: Boolean flag
  - `aiDifficulty`: Difficulty enum for AI opponent

#### 5. **Updated GameService**
- New methods:
  - `createAiGame(userId, difficulty)`: Creates a new game with AI opponent
  - `getAiMove(gameId)`: Generates and returns the AI's next move
  - `isComputerGame(gameId)`: Checks if a game is against computer

#### 6. **WebSocket Integration**
- `ChessWebSocketHandler.java` updated to:
  - Handle AI game initialization
  - Automatically generate AI moves after human player moves
  - Support both game types seamlessly

### AI Move Generation Flow

1. **Player makes a move** → Move is validated and applied to board
2. **Check if AI game** → GameService checks if game is computer game
3. **Generate AI move** → AiService runs minimax algorithm
4. **Apply AI move** → Move is validated and applied to board
5. **Update game state** → Board and game object are updated
6. **Send response** → Move is sent to player via WebSocket

## Algorithm Details

### Minimax with Alpha-Beta Pruning

```
minimax(position, depth, alpha, beta, isMaximizing):
    if depth == 0 or position is terminal:
        return evaluate(position)
    
    if isMaximizing:
        maxEval = -∞
        for each move in legal_moves:
            eval = minimax(position, depth-1, alpha, beta, false)
            maxEval = max(maxEval, eval)
            alpha = max(alpha, eval)
            if beta ≤ alpha: break (beta-cutoff)
        return maxEval
    else:
        minEval = +∞
        for each move in legal_moves:
            eval = minimax(position, depth-1, alpha, beta, true)
            minEval = min(minEval, eval)
            beta = min(beta, eval)
            if beta ≤ alpha: break (alpha-cutoff)
        return minEval
```

**Benefits**:
- Typically reduces branching factor from b to √b
- Same results as minimax but with fewer evaluations
- Allows deeper searches in the same time budget

## Usage Examples

### Starting an AI Game (WebSocket)

```json
{
  "type": "INIT_GAME",
  "userId": "player-123",
  "gameType": "HUMAN_VS_COMPUTER",
  "difficulty": "MEDIUM"
}
```

**Response:**
```json
{
  "type": "MATCHED",
  "message": "WHITE",
  "userId": "player-123",
  "opponentUserId": "computer_<game-id>",
  "gameId": "<game-id>",
  "sessionId": "<session-id>"
}
```

## Performance Considerations

### Search Space
- Chess branching factor: ~35 legal moves per position
- Depth 1: ~35 positions
- Depth 3: ~35³ ≈ 43,000 positions (with alpha-beta: ~6,400)
- Depth 5: ~35⁵ ≈ 52 million (with alpha-beta: ~1-2 million)

### Time Complexity
- **EASY**: O(b^d) where b≈35, d=1 → Very fast
- **MEDIUM**: O(b^d) where b≈35, d=3 → ~1-2 seconds
- **HARD**: O(b^d) where b≈35, d=5 → ~3-5 seconds

### Memory Usage
- Minimal: Only stores current board state being evaluated
- Board objects are copied and discarded after evaluation

## Enhancements and Future Work

### Possible Improvements

1. **Opening Book** (OpeningBook.java)
   - Precomputed opening moves for faster game start
   - Reduces search depth needed for opening phase

2. **Killer Heuristic**
   - Tracks moves that cause cutoffs at same depth
   - Improves move ordering and pruning efficiency

3. **Transposition Table**
   - Caches previously evaluated positions
   - Avoids re-evaluating same positions

4. **Iterative Deepening**
   - Search to depth 1, then 2, then 3, etc.
   - Improves time management and ensures always-fresh best move

5. **Positional Piece-Square Tables**
   - Better evaluation using piece position preferences
   - Different values for opening, middlegame, endgame

6. **Quiescence Search**
   - Extend search beyond quiet positions
   - Avoid "horizon effect" where tactics are missed

7. **Neural Network Evaluation** (Future)
   - Train on millions of games
   - Replace hand-crafted evaluation with learned function

## Testing

### Running Tests

```bash
# Run all tests except application context test
./mvnw.cmd test -Dtest="!ChessApplicationTests"

# Results: 25 tests pass
# - ChessWebSocketHandlerTest: 5 tests
# - GameControllerTest: 6 tests
# - GameServiceTest: 13 tests
# - JwtServiceTest: 1 test
```

## Configuration

### Difficulty Adjustment

To modify AI difficulty levels, edit `Difficulty.java`:

```java
public enum Difficulty {
    EASY(1, 500),           // Depth 1, 500ms max
    MEDIUM(3, 2000),        // Depth 3, 2s max
    HARD(5, 5000);          // Depth 5, 5s max
    
    // Add new level:
    // EXPERT(7, 10000)      // Depth 7, 10s max
    
    // Modify existing:
    // HARD(6, 8000)         // Increase to depth 6, 8s
}
```

### Evaluation Function Tuning

Adjust piece values and bonuses in `AiService.java`:

```java
// Material values (in getMaterialScore)
private static final int PAWN_VALUE = 1;     // Adjust these
private static final int KNIGHT_VALUE = 3;
private static final int ROOK_VALUE = 5;
private static final int QUEEN_VALUE = 9;

// Mobility and center control in evaluateMobility()
score += (currentMobility - opponentMobility) * 10;  // Weight penalty
```

## Troubleshooting

### AI Takes Too Long
- Reduce difficulty from HARD → MEDIUM → EASY
- Check `difficulty.getMaxTimeMs()` configuration
- Consider adding transposition table caching

### AI Plays Weak Moves
- Increase search depth (requires more compute time)
- Improve evaluation function with better heuristics
- Add opening book for early game

### Game Freezes After AI Move
- Ensure AI move generation runs in separate thread (already implemented)
- Check WebSocket session is still open
- Look for deadlocks in board manipulation

## Architecture Diagram

```
User → WebSocket → ChessWebSocketHandler
                        ↓
                   GameService
                        ↓
         ┌──────────────┴──────────────┐
         ↓                              ↓
    Check if AI Game          Apply/Validate Move
         ↓                              ↓
    AiService.getAiMove() → Update Game State
         ↓
    minimax(board, depth, ...)
         ↓
    Alpha-Beta Pruning
         ↓
    evaluatePosition()
         ↓
    Return best move
         ↓
    Send to User
```

## References

- **Minimax Algorithm**: https://en.wikipedia.org/wiki/Minimax
- **Alpha-Beta Pruning**: https://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning
- **Chess Engine Development**: https://www.chessprogramming.org/
- **Chesslib Library**: https://github.com/bhlangonijr/chesslib

## License

This AI implementation is part of the Chess Backend project and follows the same license as the main application.

