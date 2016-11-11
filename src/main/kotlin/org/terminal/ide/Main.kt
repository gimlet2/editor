package org.terminal.ide

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import java.io.File
import java.lang.Integer.max
import java.lang.Integer.min

/**
 * copyright (c) 2003-2015 GameDuell GmbH, All Rights Reserved
 */

fun main(argv: Array<String>) {
    val terminal = DefaultTerminalFactory().createTerminal()
    val screen = TerminalScreen(terminal)
    screen.startScreen()
    val lines: MutableList<String> = File("pom.xml").readLines().toMutableList()
    var currentRow = 0
    val space = (currentRow + screen.terminalSize.rows).toString().length

    fun drawLine(i: Int) {
        screen.newTextGraphics().putString(TerminalPosition(0, i), "${i.toString().spaces(space)} ${lines[i]}")
    }

    fun drawFirstLine(i: Int) {
        screen.newTextGraphics().putString(TerminalPosition(0, 0), "${i.toString().spaces(space)} ${lines[i]}")
    }

    fun drawLastLine(i: Int) {
        screen.newTextGraphics().putString(TerminalPosition(0, screen.terminalSize.rows - 1), "${i.toString().spaces(space)} ${lines[i]}")
    }

    fun draw() {
        for (i in lines.indices) {
            if (i < currentRow || i >= currentRow + screen.terminalSize.rows) {
                continue
            }
            drawLine(i)
        }
    }
    draw()
    screen.refresh()
    screen.cursorPosition = screen.cursorPosition.withColumn(space + 1)
    screen.refresh()
    screen.terminal.flush()
    Thread({
        while (true) {
            val input = screen.readInput()
            when (input.keyType) {
                KeyType.ArrowDown -> {
                    if (currentRow != lines.size - 1) {
                        currentRow++
                        if (screen.cursorPosition.row + 1 == screen.terminalSize.rows) {
                            screen.scrollLines(0, screen.terminalSize.rows - 1, 1)
                            drawLastLine(currentRow)
                            screen.cursorPosition = screen.cursorPosition.withColumn(min(lines[currentRow].length, screen.cursorPosition.column) + space + 1)

                        } else {
                            screen.cursorPosition = screen.cursorPosition.withRelativeRow(1).withColumn(min(lines[currentRow].length, screen.cursorPosition.column) )
                        }
                    }
                }
                KeyType.ArrowUp -> {
                    if (currentRow != 0) {
                        currentRow--
                        if (screen.cursorPosition.row == 0) {
                            screen.scrollLines(0, screen.terminalSize.rows - 1, -1)
                            drawFirstLine(currentRow)
                            screen.cursorPosition = screen.cursorPosition.withColumn(min(lines[currentRow].length, screen.cursorPosition.column) + space + 1)
                        } else {
                            screen.cursorPosition = screen.cursorPosition.withRelativeRow(-1).withColumn(min(lines[currentRow].length, screen.cursorPosition.column) + space + 1)
                        }
                    }
                }
                KeyType.ArrowLeft -> {
                    if (screen.cursorPosition.withRelativeColumn(-1).column > space)
                        screen.cursorPosition = screen.cursorPosition.withRelativeColumn(-1)
                }
                KeyType.ArrowRight -> screen.cursorPosition = screen.cursorPosition.withRelativeColumn(1)
                KeyType.Character -> {
                    lines[currentRow] = lines[currentRow].substring(0, screen.cursorPosition.column - space - 1) +
                            (if (input.isShiftDown) input.character.toUpperCase() else input.character) +
                            lines[currentRow].substring(screen.cursorPosition.column - space - 1)
                    drawLine(currentRow)
                }
            }
            screen.refresh()
        }
    }).start()
}

fun detectTerminalSize(): Size {
    fun tput(dm: String): Int {
        return Runtime.getRuntime().exec(arrayOf("bash", "-c", "tput $dm 2> /dev/tty")).inputStream.bufferedReader().readLine().trim().toInt()
    }
    return Size(tput("cols"), tput("lines"))
}


data class Size(val cols: Int, val rows: Int)

fun String.spaces(length: Int): String {
    if (this.length < length) {
        val sb: StringBuffer = StringBuffer(this)
        while (sb.length < length) {
            sb.append(" ")
        }
        return sb.toString()
    }
    return this
}