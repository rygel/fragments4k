@param io.andromeda.fragments.FragmentViewModel fragment

<article hx-target="main" hx-swap="innerHTML">
    <header>
        <h1>${fragment.title}</h1>
        @if(fragment.date != null)
            <time>${fragment.date.toString()}</time>
        @endif
    </header>
    <div class="content">
        ${fragment.content}
    </div>
</article>
