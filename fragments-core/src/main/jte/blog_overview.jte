@param List<io.andromeda.fragments.FragmentViewModel> fragments
@param int currentPage
@param int totalPages
@param boolean hasNext
@param boolean hasPrevious

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Blog</title>
    <script src="https://unpkg.com/htmx.org@1.9.10"></script>
</head>
<body>
    <main>
        <h1>Blog</h1>
        <div class="blog-posts">
            @for(fragment : fragments)
                <article class="post-preview" hx-target="main" hx-swap="innerHTML">
                    <h2>
                        <a href="/blog/${fragment.year}/${fragment.month}/${fragment.slug}" hx-push-url="true">
                            ${fragment.title}
                        </a>
                    </h2>
                    <div class="preview">
                        ${fragment.preview}
                    </div>
                    <a href="/blog/${fragment.year}/${fragment.month}/${fragment.slug}" class="read-more">
                        Read more
                    </a>
                </article>
            @endfor
        </div>
        
        @if(totalPages > 1)
            <nav class="pagination">
                @if(hasPrevious)
                    <a href="/blog/page/${currentPage - 1}" class="prev">Previous</a>
                @endif
                <span>Page ${currentPage} of ${totalPages}</span>
                @if(hasNext)
                    <a href="/blog/page/${currentPage + 1}" class="next">Next</a>
                @endif
            </nav>
        @endif
    </main>
</body>
</html>
