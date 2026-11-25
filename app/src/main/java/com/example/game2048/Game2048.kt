package com.example.game2048

import kotlin.random.Random

enum class Move { UP, DOWN, LEFT, RIGHT }

// Поле 4x4: 0 — пустая клетка, иначе значение плитки (2, 4, 8, ...)
data class Board(
    val cells: List<List<Int>>,
    val score: Int = 0,
    val won: Boolean = false
) {
    val size get() = cells.size
    val maxTile get() = cells.flatten().max()
    val isGameOver: Boolean get() = Move.values().none { Game2048.move(this, it).moved }
}

data class MoveResult(val board: Board, val moved: Boolean, val gained: Int)

object Game2048 {
    const val SIZE = 4
    const val WIN = 2048

    fun newGame(random: Random = Random): Board {
        var b = Board(List(SIZE) { List(SIZE) { 0 } })
        b = spawn(b, random); b = spawn(b, random)
        return b
    }

    // Новая плитка в случайной пустой клетке: 2 (90%) или 4 (10%)
    fun spawn(b: Board, random: Random = Random): Board {
        val empty = b.cells.indices.flatMap { r -> b.cells[r].indices.filter { b.cells[r][it] == 0 }.map { r to it } }
        if (empty.isEmpty()) return b
        val (r, c) = empty[random.nextInt(empty.size)]
        val v = if (random.nextInt(10) == 0) 4 else 2
        return b.copy(cells = b.cells.mapIndexed { i, row -> if (i != r) row else row.mapIndexed { j, x -> if (j == c) v else x } })
    }

    // Сдвиг одной строки влево: убрать нули, слить пары (каждая плитка — один раз за ход)
    fun slideRow(row: List<Int>): Pair<List<Int>, Int> {
        val tiles = row.filter { it != 0 }
        val out = mutableListOf<Int>()
        var gained = 0
        var i = 0
        while (i < tiles.size) {
            if (i + 1 < tiles.size && tiles[i] == tiles[i + 1]) {
                val merged = tiles[i] * 2
                out += merged; gained += merged; i += 2
            } else {
                out += tiles[i]; i += 1
            }
        }
        while (out.size < row.size) out += 0
        return out to gained
    }

    private fun transpose(m: List<List<Int>>) = m.indices.map { c -> m.indices.map { r -> m[r][c] } }

    // Ход без появления новой плитки
    fun move(b: Board, dir: Move): MoveResult {
        // Сводим любой ход к сдвигу влево
        val prepared = when (dir) {
            Move.LEFT -> b.cells
            Move.RIGHT -> b.cells.map { it.reversed() }
            Move.UP -> transpose(b.cells)
            Move.DOWN -> transpose(b.cells).map { it.reversed() }
        }
        var gained = 0
        val slid = prepared.map { val (r, g) = slideRow(it); gained += g; r }
        val result = when (dir) {
            Move.LEFT -> slid
            Move.RIGHT -> slid.map { it.reversed() }
            Move.UP -> transpose(slid)
            Move.DOWN -> transpose(slid.map { it.reversed() })
        }
        val moved = result != b.cells
        val nb = b.copy(cells = result, score = b.score + gained, won = b.won || result.flatten().any { it >= WIN })
        return MoveResult(nb, moved, gained)
    }

    // Полный ход игрока: сдвиг + новая плитка (если что-то сдвинулось)
    fun play(b: Board, dir: Move, random: Random = Random): MoveResult {
        val r = move(b, dir)
        return if (r.moved) r.copy(board = spawn(r.board, random)) else r
    }
}
