@param io.andromeda.fragments.FragmentViewModel fragment

<!DOCTYPE html>
<html lang="${fragment.language}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${fragment.title}</title>
    <script src="https://unpkg.com/htmx.org@1.9.10"></script>
</head>
<body>
    <main>
        <article>
            <header>
                <h1>${fragment.title}</h1>
                @if(fragment.date != null)
                    <time>${fragment.date.toString()}</time>
                @endif
            </header>
            <div class="content">
                ${fragment.content}
            </div>
            @if(!fragment.tags.isEmpty())
                <footer>
                    <div class="tags">
                        @for(tag : fragment.tags)
                            <a href="/blog/tag/${tag}" class="tag">#${tag}</a>
                        @endfor
                    </div>
                </footer>
            @endif
        </article>
    </main>
</body>
</html>
