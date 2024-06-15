import {fromMarkdown} from 'mdast-util-from-markdown'

main()

function main()
{
    if (process.argv.length < 3)
        throw "No input"

    const markdown_string = process.argv[2] 
    const mdast = fromMarkdown(markdown_string)

    console.log(JSON.stringify(mdast))
}