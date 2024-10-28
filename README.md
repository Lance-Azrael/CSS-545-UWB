# EasyScreen Translator

Dingyuan Xue, Sicen Liu 
Version #2

## Summary of Project

Easyscreen Translator offers a one-click feature to translate screen content and supports selecting the specific area of the screen that needs translation.

## Project Analysis

### Value Proposition

Most translation apps require manual text input, which can be tedious when dealing with non-text formats like images on mobile devices. While tools like Google Translate allow photo translation, if you want to translate what's on the screen, the process involves multiple steps—capturing, cropping, switching apps, and importing. EasyScreen Translator streamlines the entire experience into a single step, saving users time and hassle.

### Primary Purpose

EasyScreen translator provides users with a one-click feature to translate screen content.

### Target Audience

The target demographic is beginners in foreign languages, especially university students, who need to read much foreign literature on mobile devices (especially on a tablet computer). University students often need to read PDF papers and formatted web materials, which are inconvenient for copying text. Additionally, for beginners, switching between the original text and the translated content occurs frequently. Promotion can be conducted on commonly used resource-sharing platforms to reach this group.

### Success Criteria

1. It fully realizes the function. 
2. It achieves at least an 80% satisfaction rate in user feedback within the first three months.
3. It attains a conversion rate of at least 10% from free trial users to paid subscribers within the first three months.
4. It reaches a minimum of 1000 unique users within the first year.

### Competitor Analysis

Google Translate offers more features, such as conversation translation and handwriting recognition, as well as additional information about the translated content, like synonyms and example sentences. However, translating screen content in Google involves several complicated steps—capturing, cropping, switching apps, and importing, making it a cumbersome process.

### Monetization Model

Freeminum. Provide users with a certain number of free trial uses, after which they must purchase the software features for continued access.

## Initial Design

### UI/UX Design


Initial Page
1. Dropdown menu to select the translation language.
2. Start button, click to show/hide the floating button.
3. User button, click to navigate to the user interface.

User Page:
1. Log in using the login button.
2. Enter and display user name, email, and VIP status.
3. Use the edit button to edit user information.
4. Use the logout button to log out.

On top of other applications:
1. Floating button, click to enter translation mode.
2. In translation mode, swipe to select the translation area.
3. Translations are displayed at the bottom of the screen.
4. Return button, click to return normal mode.

<img src="https://github.com/Lance-Azrael/CSS-545-UWB/blob/main/CP2%20image.png?raw=true" alt="img" style="zoom:25%;" />


### Technical Architecture

1. A button that can float on top of other applications.
2. A component to control the screenshot function and clipping function. Use Media Projections.
3. A component to control recognizing text from the screenshot image. Use Google Dependencies.
4. A component to control translating the text. Use Google Dependencies. 

## Challenges and Open Questions

1. The acquisition of permissions such as capturing the screen, floating window, storage and background running. Active the Start and Stop button only after obtaining these permissions. 
2. Getting the area the user wants to translate. Use the screenshot function of the system. May record the position where the user has touched to clip the screenshot image. 
3. Privacy protection. Inform users in advance if the content needs to be uploaded. Store all data such as images locally. Do not record anything when the user clicks the screenshot button. 

Question: Does Android allow an app to do the screenshot without using the Android system's own screenshot function? Are there any possible solutions to implement a self-defined screenshot function and are there any apps did this before?
