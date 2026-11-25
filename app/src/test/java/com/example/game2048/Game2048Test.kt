package com.example.game2048

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class Game2048Test {
    private fun b(vararg rows: List<Int>) = Board(rows.toList())

    @Test fun newGameHasTwoTiles() {
        val g = Game2048.newGame(Random(1))
        assertEquals(2, g.cells.flatten().count { it != 0 })
        assertEquals(4, g.size)
    }

    @Test fun slideMergesPairs() {
        assertEquals(listOf(4, 0, 0, 0) to 4, Game2048.slideRow(listOf(2, 0, 2, 0)))
        assertEquals(listOf(4, 4, 0, 0) to 8, Game2048.slideRow(listOf(2, 2, 2, 2)))   // каждая плитка сливается один раз
        assertEquals(listOf(8, 4, 0, 0) to 8, Game2048.slideRow(listOf(4, 4, 4, 0)))   // сливается первая пара
        assertEquals(listOf(2, 4, 2, 0) to 0, Game2048.slideRow(listOf(2, 4, 2, 0)))
    }

    @Test fun moveRightAndDown() {
        val board = b(listOf(2, 2, 0, 0), listOf(0, 0, 0, 0), listOf(2, 0, 0, 0), listOf(0, 0, 0, 0))
        val right = Game2048.move(board, Move.RIGHT)
        assertEquals(listOf(0, 0, 0, 4), right.board.cells[0])
        assertEquals(4, right.board.score)
        val down = Game2048.move(board, Move.DOWN)
        assertEquals(4, down.board.cells[3][0])  // две двойки в первом столбце слились внизу
        assertEquals(2, down.board.cells[3][1])
    }

    @Test fun noMoveNoSpawn() {
        val board = b(listOf(2, 4, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0))
        val r = Game2048.play(board, Move.LEFT)
        assertFalse(r.moved)
        assertEquals(board.cells, r.board.cells)
    }

    @Test fun moveSpawnsTile() {
        val board = b(listOf(0, 0, 0, 2), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0))
        val r = Game2048.play(board, Move.LEFT, Random(3))
        assertTrue(r.moved)
        assertEquals(2, r.board.cells.flatten().count { it != 0 })
    }

    @Test fun gameOverDetected() {
        val full = b(listOf(2, 4, 2, 4), listOf(4, 2, 4, 2), listOf(2, 4, 2, 4), listOf(4, 2, 4, 2))
        assertTrue(full.isGameOver)
        val canMerge = b(listOf(2, 2, 4, 8), listOf(4, 8, 16, 32), listOf(8, 16, 32, 64), listOf(16, 32, 64, 128))
        assertFalse(canMerge.isGameOver)
    }

    @Test fun reaching2048Wins() {
        val board = b(listOf(1024, 1024, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0))
        val r = Game2048.move(board, Move.LEFT)
        assertTrue(r.board.won)
        assertEquals(2048, r.board.maxTile)
    }
}
