import {toMarkdown} from 'mdast-util-to-markdown'

main()

function main()
{
    if (process.argv.length < 3)
        throw "No input"

    const md_ast = JSON.parse(process.argv[2])
    const markdown = toMarkdown(md_ast)

    console.log(markdown)
}
