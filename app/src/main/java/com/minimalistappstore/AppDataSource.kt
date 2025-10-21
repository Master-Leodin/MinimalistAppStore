package com.minimalistappstore

object AppDataSource {
    fun getApps(): List<App> {
        return listOf(
            App(
                name = "Focus Zen",
                developer = "Indie Studio",
                description = "Um timer minimalista para a técnica Pomodoro. Sem distrações, apenas foco. Desenvolvido para ajudar você a conquistar suas tarefas com calma e eficiência.",
                iconUrl = "https://i.imgur.com/g7p0a1L.png", // Ícone de exemplo
                apkUrl = "https://example.com/focus-zen.apk",
                version = "1.2.0",
                openSourceUrl = "https://github.com/indiestudio/focus-zen"
            ),
            App(
                name = "Plain Notes",
                developer = "Simple Tools Co.",
                description = "O bloco de notas mais simples que você encontrará. Abra, escreva, feche. Sincronização na nuvem opcional e sem anúncios. Suas ideias, em sua forma mais pura.",
                iconUrl = "https://i.imgur.com/KY2jA9k.png", // Ícone de exemplo
                apkUrl = "https://example.com/plain-notes.apk",
                version = "3.0.1",
                openSourceUrl = "https://github.com/simpletools/plain-notes"
            ),
            App(
                name = "Monochrome Wallpapers",
                developer = "Artist Collective",
                description = "Uma coleção curada de wallpapers em tons de cinza. Perfeito para uma tela limpa e elegante. Novas imagens adicionadas semanalmente por artistas independentes.",
                iconUrl = "https://i.imgur.com/uJpN5hW.png", // Ícone de exemplo
                apkUrl = "https://example.com/monochrome-walls.apk",
                version = "1.0.5",
                openSourceUrl = "https://github.com/artistcollec/monochrome-walls"
            )
        )
    }
}