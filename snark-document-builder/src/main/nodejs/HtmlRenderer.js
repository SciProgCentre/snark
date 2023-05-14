import {toHast} from 'mdast-util-to-hast'
import {toHtml} from 'hast-util-to-html'

main()

function main()
{
    if (process.argv.length < 3)
        throw "No input"

    const md_ast = JSON.parse(process.argv[2])
    const hast = toHast(md_ast)
    const html = toHtml(hast)

    console.log(html)
}
