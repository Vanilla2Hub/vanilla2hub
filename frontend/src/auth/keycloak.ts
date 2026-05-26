import Keycloak from 'keycloak-js'

const keycloak = new Keycloak({
  url: 'https://keycloak.hjeon.i234.me',
  realm: 'vanilla2hub',
  clientId: 'vanilla2hub-frontend',
})

export default keycloak
