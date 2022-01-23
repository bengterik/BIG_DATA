#
# This is a Shiny web application. You can run the application by clicking
# the 'Run App' button above.
#
# Find out more about building applications with Shiny here:
#
#    http://shiny.rstudio.com/
#


#install.packages("dplyr")

#source("covid/helpers.R")
# tryCatch(
#   {library(shiny)},
#   finally = install.packages("shiny")
# )
# tryCatch(
#   {library(maps)},
#   finally = install.packages("tmap")
# )
# tryCatch(
#   {library(mapproj)},
#   finally = install.packages("mapproj")
# )
# tryCatch(
#   {library(rworldmap)},
#   finally = install.packages("rworldmap")
# )
source("helpers.R")
library(shiny)

# library(maps)
# library(mapproj)
library(leaflet.providers)
data <- read.csv("Data/deaths_covid.csv")
all_columns <- colnames(data, do.NULL = TRUE, prefix = "col")
columns_wanted <- c("iso_code", "continent", "location", "date",
                   "total_cases_per_million", "total_deaths_per_million", 
                   "reproduction_rate", "total_boosters_per_hundred", 
                   "aged_65_older")
data_wanted <- data[columns_wanted]
# get the dates as a column to return the first and last day
dates <- as.Date(data_test_wanted_columns$date)
first_day <- min(dates)
last_day <- max(dates)
chosen_day <- first_day
#continents <- dplyr::distinct(data_wanted, continent)[!apply(is.na(dplyr::distinct(data_wanted, continent)) | dplyr::distinct(data_wanted, continent) == "", 1, all),]



# Define UI for application that draws a histogram
ui <- fluidPage(
   
   # Application title
   titlePanel("Covid Data"),
   
   # Sidebar with a slider input for the choosen date
   
   sidebarLayout(
      sidebarPanel(
        fluidRow(
          column(10,sliderInput("date",
                     "Choose the date:",
                     min = first_day,
                     max = last_day,
                     value = chosen_day))),
         fluidRow(
           column(10,
                  selectInput("chosen_variable", h3("Desired variable:"),
                              choices = list("total_cases_per_million" = 1,
                                             "total_deaths_per_million" = 2,
                                             "reproduction_rate" = 3,
                                             "total_boosters_per_hundred" = 4, 
                                             "aged_65_older" = 5)), selected = 1)),
        fluidRow(
          column(10,
                 selectInput("chosen_continent", h3("Choose the continent:"),
                             choices = list("Africa" = 1,
                                            "Asia" = 2,
                                            "Europe" = 3,
                                            "North America" = 4,
                                            "South America" = 5,
                                            "Oceania" = 6,
                                            "The whole world" = 7)), selected = 7))),
        mainPanel(
          plotOutput(outputId = "map")
        )
      )
)
     


# Define server logic required to draw a histogram
server <- function(input, output) {
  output$map <- renderPlot({
    data <- switch(input$chosen_variable, 
                   "total_cases_per_million" = data_wanted$total_cases_per_million,
                   "total_deaths_per_million" = data_wanted$total_deaths_per_million,
                   "reproduction_rate" = data_wanted$reproduction_rate,
                   "total_boosters_per_hundred" = data_wanted$total_boosters_per_hundred, 
                   "aged_65_older" = data_wanted$aged_65_older)
    
    color <- switch(input$chosen_variable, 
                    "total_cases_per_million" = "darkorange4",
                    "total_deaths_per_million" = "darkred",
                    "reproduction_rate" = "dodgerblue4",
                    "total_boosters_per_hundred" = "forestgreen", 
                    "aged_65_older" = "antiquewhite4")
    
    legend <- switch(input$var, 
                     "total_cases_per_million" = "total cases per million",
                     "total_deaths_per_million" = "total deaths per million",
                     "reproduction_rate" = "reproduction rate",
                     "total_boosters_per_hundred" = "total boosters per hundred", 
                     "aged_65_older" = "aged 65 older")
    
    percent_map(data, color, legend, input$date)
  })
}

# Run the application 
shinyApp(ui = ui, server = server)

