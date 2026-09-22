import type { Preview } from '@storybook/react-vite'
import '../src/styles/tokens.css'

const preview: Preview = {
  parameters: {
    controls: { matchers: { date: /Date$/i } },
    layout: 'padded',
  },
}
export default preview
